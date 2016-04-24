def aaa():
    print 'aaa 1'
    a = bbb() + eee()
    print 'aaa 2'
    return a

def bbb():
    print 'bbb 1'
    b = ccc() + ddd()
    print 'bbb 2'
    return b

def ccc():
    print 'ccc 1'
    c = 1
    print 'ccc 2'
    return c

def ddd():
    print 'ddd 1'
    d = 0
    print 'ddd 2'
    return d

def eee():
    print 'eee 1'
    e = 1
    print 'eee 2'
    return e

f = aaa()
print f