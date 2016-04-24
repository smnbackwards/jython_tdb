a, b, c, d = 0,0,0,0
i = 0
while i < 1000:
    a += i % 4
    b += i+1 % 4
    c += i+2 % 4
    d += i+3 % 4
    i+=1
    
print a,b,c,d
