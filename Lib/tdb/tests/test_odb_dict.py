import _odb as odbMod
import unittest
from test import test_support

_odb = odbMod.test_odb()


class DictTestCase(unittest.TestCase):
    def _test_dict(self, actions, initial_value={}):
        '''
        Runs each action on a dict with recording and compares the list to
        the same actions run on a regular list
        Each action is performed on a consecutive timestamp
        '''
        _odb.enabled = 0
        returnValues = []

        reference_list = dict(initial_value)
        _odb.enabled = 1
        _odb.currentTimestamp = 0
        history_list = dict(initial_value)

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
            self.assertEqual(reference_list, history_list)
            _odb.currentTimestamp += 1

        _odb.replaying = 0

    def test_set(self):
        self._test_dict([
            (lambda d: d.__setitem__('key','value')),
            (lambda d: d.__setitem__('key','newvalue')),
            (lambda d: d.clear),
            (lambda d: d.__setitem__('key','after clear')),
        ])

    def test_update(self):
        self._test_dict([
            (lambda d: d.__setitem__('key','value')),
            (lambda d: d.update({'key':'newvalue'})),
            (lambda d: d.clear),
            (lambda d: d.__setitem__('key','after clear')),
            (lambda d: d.update({'key':'newvalue after clear'})),

        ])

    def test_del(self):
        self._test_dict([
            (lambda d: d.__setitem__('key','value')),
            (lambda d: d.__setitem__('key','newvalue')),
            (lambda d: d.__delitem__('key')),
            (lambda d: d.__setitem__('key','after del')),
        ])

    def test_set_many(self):
        self._test_dict([
            (lambda d: d.__setitem__('key','value')),
            (lambda d: d.__setitem__('key','newvalue')),
            (lambda d: d.clear),
            (lambda d: d.__setitem__('key','after clear')),
        ], {'one':'jan', 'two':'feb', 'three':'mar'})

    def test_update_many(self):
        self._test_dict([
            (lambda d: d.update({'one':'1', 'three':'3'})),
            (lambda d: d.clear),
            (lambda d: d.__setitem__('key','after clear')),
            (lambda d: d.update({'key':'newvalue after clear'})),
        ], {'one':'jan', 'two':'feb', 'three':'mar'})

    def test_del_many(self):
        self._test_dict([
            (lambda d: d.__setitem__('key','value')),
            (lambda d: d.__setitem__('key','newvalue')),
            (lambda d: d.__delitem__('key')),
            (lambda d: d.__setitem__('key','after clear')),
        ], {'one':'jan', 'two':'feb', 'three':'mar'})

    def test_delete_manual(self):
        _odb.enabled = 1
        _odb.currentTimestamp = 0
        d = dict({})

        _odb.currentTimestamp = 1
        d.__setitem__('key','value') ; _odb.currentTimestamp = 2
        d.__setitem__('key','newvalue'); _odb.currentTimestamp = 3
        d.__delitem__('key') ; _odb.currentTimestamp = 4
        d.__setitem__('key','after del') ; _odb.currentTimestamp+= 5

        _odb.enabled = 0
        _odb.replaying = 1

        _odb.currentTimestamp = 0 ; self.assertEqual({}, d)
        _odb.currentTimestamp = 1 ; self.assertEqual({'key':'value'}, d)
        _odb.currentTimestamp = 2 ; self.assertEqual({'key':'newvalue'}, d)
        _odb.currentTimestamp = 3 ; self.assertEqual({}, d)
        _odb.currentTimestamp = 4 ; self.assertEqual({'key':'after del'}, d)

        _odb.replaying = 0

def test_main():
    test_support.verbose = 1
    test_support.run_unittest(DictTestCase)

if __name__ == '__main__':
    test_main()
