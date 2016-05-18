# Stress tests odb to see how many events it can store before running out of memory
i = 0
while i < 2**25 : #19 seems to be the limit
    i = i + 1