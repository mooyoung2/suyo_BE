import csv, bisect
from collections import defaultdict

sales=list(csv.DictReader(open('seoul_sales_quarterly.csv',encoding='utf-8')))
stores=list(csv.DictReader(open('store_counts_by_sgg.csv',encoding='utf-8')))
codes={r['소분류코드']:r for r in csv.DictReader(open('industry_codes.csv',encoding='utf-8'))}
sur={r['상가업종대분류코드']:r for r in csv.DictReader(open('industry_survival_rates.csv',encoding='utf-8'))}
mapping=list(csv.DictReader(open('industry_code_mapping.csv',encoding='utf-8')))

quarters=sorted(set(r['기준분기'] for r in sales)); L4=quarters[-4:]; F4=quarters[:4]; P4=quarters[-8:-4]
YEARS=(len(quarters)-4)/4
by=defaultdict(dict)
for r in sales: by[r['업종명']][r['기준분기']]=r

# ---------- 업종별 원지표 계산 ----------
ind_metrics={}
for n,qs in by.items():
    recA=sum(int(qs[q]['매출금액']) for q in L4); oldA=sum(int(qs[q]['매출금액']) for q in F4); preA=sum(int(qs[q]['매출금액']) for q in P4)
    recC=sum(int(qs[q]['매출건수']) for q in L4); oldC=sum(int(qs[q]['매출건수']) for q in F4)
    last=qs[quarters[-1]]
    m,f=int(last['남성매출']),int(last['여성매출'])
    ages=[int(last[k]) for k in ['연령10','연령20','연령30','연령40','연령50','연령60이상']]
    wd,we=int(last['주중매출']),int(last['주말매출'])
    ind_metrics[n]=dict(
        size=recA,
        cagr=((recA/oldA)**(1/YEARS)-1)*100 if oldA>0 else 0,
        momentum=((recA/preA-1)*100 - (((recA/oldA)**(1/YEARS)-1)*100)) if preA>0 and oldA>0 else 0,
        demand=((recC/oldC)**(1/YEARS)-1)*100 if oldC>0 else 0,
        gender=max(m,f)/(m+f)*100 if m+f else 50,
        age=max(ages)/sum(ages)*100 if sum(ages) else 100,
        weekend=we/(wd+we)*100 if wd+we else 0,
    )

# ---------- 백분위 변환기 ----------
class Rank:
    def __init__(self, vals, higher_is_better=True):
        self.v=sorted(vals); self.h=higher_is_better
    def pct(self, x):
        i=bisect.bisect_left(self.v,x); p=i/(len(self.v)-1)*100
        return p if self.h else 100-p

R={}
for k,hib in [('size',True),('cagr',True),('momentum',True),('demand',True)]:
    R[k]=Rank([v[k] for v in ind_metrics.values()], hib)
# 쏠림은 낮을수록 좋음
R['gender']=Rank([v['gender'] for v in ind_metrics.values()], False)
R['age']=Rank([v['age'] for v in ind_metrics.values()], False)
# 주말비중은 중앙에서 멀수록 나쁨 → 중앙값과의 거리로 변환
wk=[v['weekend'] for v in ind_metrics.values()]
wk_med=sorted(wk)[len(wk)//2]
R['weekend']=Rank([abs(v-wk_med) for v in wk], False)

# L3용
by_code=defaultdict(dict)
for r in stores: by_code[r['소분류코드']][r['시군구명']]=int(r['사업체수'])
ratios=[]
for c,g in by_code.items():
    avg=sum(g.values())/len(g)
    if avg>0:
        for gu,v in g.items(): ratios.append(v/avg)
R['density']=Rank(ratios, False)   # 밀도 낮을수록 좋음
R['survival']=Rank([float(r['5년생존율']) for r in sur.values() if r['상가업종대분류코드']], True)

MAX=dict(size=10,cagr=15,momentum=5,gender=7,age=7,demand=11,weekend=15,density=15,survival=15)
def sc(key,x): return R[key].pct(x)/100*MAX[key]

# ---------- 전체 조합 점수 계산 ----------
sales_name_by_small={}
for r in mapping: sales_name_by_small[r['상가소분류코드']]=r['매출업종명']

results=[]
for small_code, gus in by_code.items():
    sname=sales_name_by_small.get(small_code)
    if not sname or sname not in ind_metrics: continue
    M=ind_metrics[sname]
    l1=sc('size',M['size'])+sc('cagr',M['cagr'])+sc('momentum',M['momentum'])
    l2=sc('gender',M['gender'])+sc('age',M['age'])+sc('demand',M['demand'])+sc('weekend',abs(M['weekend']-wk_med))
    avg=sum(gus.values())/len(gus)
    lc=codes[small_code]['대분류코드']; s5=float(sur[lc]['5년생존율'])
    for gu,n in gus.items():
        ratio=n/avg if avg>0 else 1
        dens = sc('density',ratio) if n>=10 else MAX['density']*0.5
        l3=dens+sc('survival',s5)
        results.append(dict(code=small_code, name=codes[small_code]['소분류명'], gu=gu, n=n,
                            l1=l1,l2=l2,l3=l3,total=l1+l2+l3))

print(f'점수 산출 가능 조합: {len(results):,}개')
def P(vals,p):
    v=sorted(vals); k=(len(v)-1)*p/100; f=int(k); c=min(f+1,len(v)-1)
    return v[f]+(v[c]-v[f])*(k-f)

for key,mx in [('l1',30),('l2',40),('l3',30),('total',100)]:
    vals=[r[key] for r in results]
    print(f'\n[{key.upper()}] 만점 {mx} — 실제 분포')
    for p in [5,10,25,33,50,67,75,90,95]:
        print(f'   P{p:>2}: {P(vals,p):5.1f}점', end='')
        if p in (25,33,67,75): print('  ←', end='')
        print()

print('\n\n================ 등급 경계 (3분위) ================')
cuts={}
for key,mx in [('l1',30),('l2',40),('l3',30),('total',100)]:
    vals=[r[key] for r in results]
    lo,hi=P(vals,33),P(vals,67)
    cuts[key]=(lo,hi)
    print(f'{key.upper():6s} 만점{mx:3d} | 위험 <{lo:.1f} | 보통 {lo:.1f}~{hi:.1f} | 안전 ≥{hi:.1f}')

def gr(key,v):
    lo,hi=cuts[key]
    return '안전' if v>=hi else ('보통' if v>=lo else '위험')

print('\n================ 등급 분포 검증 ================')
for key in ['l1','l2','l3','total']:
    c={'안전':0,'보통':0,'위험':0}
    for r in results: c[gr(key,r[key])]+=1
    t=len(results)
    print(f'{key.upper():6s} 안전 {c["안전"]/t*100:.1f}% / 보통 {c["보통"]/t*100:.1f}% / 위험 {c["위험"]/t*100:.1f}%')

print('\n================ 사례 ================')
for nm,gu in [('카페','중랑구'),('카페','강남구'),('카페','종로구'),('일반의원','중랑구'),('편의점','강남구')]:
    hit=[r for r in results if r['name']==nm and r['gu']==gu]
    if not hit: 
        print(f'{nm}/{gu}: 매핑 없음'); continue
    r=hit[0]
    print(f"{nm}/{gu} (점포 {r['n']}개): L1 {r['l1']:.1f}({gr('l1',r['l1'])}) L2 {r['l2']:.1f}({gr('l2',r['l2'])}) L3 {r['l3']:.1f}({gr('l3',r['l3'])}) → 총 {r['total']:.1f}/100 {gr('total',r['total'])}")

print('\n================ 최고/최저 ================')
srt=sorted(results,key=lambda x:-x['total'])
for r in srt[:3]: print(f"  최상위 {r['name']}/{r['gu']}: {r['total']:.1f}")
for r in srt[-3:]: print(f"  최하위 {r['name']}/{r['gu']}: {r['total']:.1f}")
