l = []

l.append('a')
l.append('c')
l.insert(1, 'b')

l[0] = 'A'
l[1] = 'B'
l[2] = 'C'

l.remove('A')
del l[0]
l.extend(['3','2','1'])
l.sort()
l + ['x','y','z']