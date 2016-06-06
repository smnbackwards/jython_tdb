def fib(n):
    if n <= 1 :
        return n
    f1 = fib(n-1)
    f2 = fib(n-2)
    return f1 + f2
f = fib(3)
print f