
def catchException():
    try:
        raise Exception('caught exception')
    except Exception:
        pass

def raisesException():
    a = catchException + 1


catchException()
try:
    raisesException()
except AssertionError as e:
    print e
except TypeError as f:
    print f
try:
    pass
except Exception:
    pass
else :
    print 'there was no exception'

try:
    pass
except Exception:
    pass
else :
    print 'there was no exception but'
finally:
    print 'we do have a finally'

error = 'a'/2