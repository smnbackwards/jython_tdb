def binarySearch(alist, item):
    first = 0
    last = len(alist) -1

    while first < last:
        mid = (first + last) // 2
        miditem = alist[mid]

        if miditem < item:
            first = mid + 1
        elif miditem > item :
            last = mid - 1
        else :
            return mid

    return -1


testlist = [3, 4, 6, 7, 10, 11, 34, 67, 84]
print testlist
print binarySearch(testlist, 4)
print binarySearch(testlist, 6)
print binarySearch(testlist, 7)

