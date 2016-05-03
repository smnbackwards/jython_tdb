import _odb
import unittest
from test import test_support

LIST = type([])


class ListTestCase(unittest.TestCase):
    def _test_list(self, actions, initial_value=[]):
        '''
        Runs each action on a list with recording and compares the list to
        the same actions run on a regular list
        Each action is performed on a consecutive timestamp
        '''

        returnValues = []

        reference_list = initial_value[:]
        _odb.enabled = 1
        _odb.currentTimestamp = 0
        history_list = initial_value[:]

        # Perform all actions on the history list
        for f in actions:
            _odb.currentTimestamp += 1
            retVal = f(history_list)
            returnValues.append(retVal)

        _odb.enabled = 0
        _odb.replaying = 1
        _odb.currentTimestamp = 1

        for f in actions:
            returnValue = f(reference_list)
            self.assertEqual(returnValue, returnValues[_odb.currentTimestamp - 1])
            self.assertSequenceEqual(reference_list, history_list)
            _odb.currentTimestamp += 1

        _odb.replaying = 0



    def test_append(self):
        self._test_list([
            (lambda l: l.append('a')),
            (lambda l: l.append('b')),
        ])

    def test_extend(self):
        self._test_list([
            (lambda l: l.append('a')),
            (lambda l: l.extend(['b', 'c'])),
            (lambda l: l.extend(['d', 'e', 'f'])),
            (lambda l: l.extend([])),
        ])

    def test_insert(self):
        self._test_list([
            (lambda l: l.insert(0, 'start')),
            (lambda l: l.insert(6, 'middle')),
            (lambda l: l.insert(9, 'end')),
        ],
            [1, 236, 3, 44, 75, 6, 7]
        )

    def test_remove(self):
        self._test_list([
            (lambda l: l.remove(1)),
            (lambda l: l.remove(7)),
            (lambda l: l.remove(44)),
        ],
            [1, 236, 3, 44, 75, 6, 7]
        )

    def test_pop(self):
        self._test_list([
            (lambda l: l.pop()),
            (lambda l: l.pop(1)),
            (lambda l: l.pop(4)),
        ],
            [1, 236, 3, 44, 75, 6, 7]
        )

    def test_index(self):
        self._test_list([
            (lambda l: l.index(1)),
            (lambda l: l.index(44)),
            (lambda l: l.index(7)),
        ],
            [1, 236, 3, 44, 75, 6, 7]
        )

    def test_count(self):
        self._test_list(
            [
                (lambda l: l.count(4)),
                (lambda l: l.count(2)),
                (lambda l: l.count(999)),
            ],
            [2, 3, 5, 4, 1, 4, 7, 4, 98, 5, 34, 35, 0]
        )

    def test_sort(self):
        self._test_list(
            [(lambda l: l.sort())],
            [2, 3, 5, 73, 1, 8, 7, 4, 98, 5, 34, 35, 0]
        )

    def test_reverse(self):
        self._test_list(
            [(lambda l: l.reverse())],
            [1, 2, 3, 4, 5]
        )

    def test_set(self):
        self._test_list(
            [
                (lambda l: l.__setitem__(0, 'a')), #l[0]=a is not a valid lambda
                (lambda l: l.__setitem__(4, 'b'))
            ],
            [1, 2, 3, 4, 5]
        )

    def test_get(self):
        self._test_list(
            [
                (lambda l: l[0]),
                (lambda l: l[4]),
        ],
        [1, 2, 3, 4, 5]
        )

# TODO test slicing
# TODO test scenario's after removing elements from the list
# TODO test scenarios with many different levels of removed elements
# TODO test iterator
# TODO test various list(..) functions
# TODO test lists of more complex objects to make sure fields and references are handled correctly

def test_main():
    test_support.verbose = 1
    test_support.run_unittest(ListTestCase)

if __name__ == '__main__':
    test_main()
