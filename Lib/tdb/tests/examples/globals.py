a = 0
def f():
    global a
    a = 2
    a = 3
    a = 4
    
a = 1
f()
a = 5