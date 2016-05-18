# Stress tests odb to see how many frames it can store before running out of memory
def call():
        pass
i = 0
while i < 2**24 : #limit between 23 and 24
    call()
    i = i + 1