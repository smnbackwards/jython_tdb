import _odb
import unittest
from test import test_support

LIST = type([])


class C():
    field = "a field"

    def __init__(self):
        self.int = 1
        self.object = None
        self.string = "this is a string"

class InstanceTestCase(unittest.TestCase):

    def test_primitive(self):
        ref = C()
        _odb.enabled = 1
        _odb.currentTimestamp = 0
        h = C()

        # Perform all actions on the history object
        _odb.currentTimestamp += 1
        h.int = 2
        _odb.currentTimestamp += 1
        h.object = h
        _odb.currentTimestamp += 1
        h.string = h.field
        _odb.currentTimestamp += 1
        h.field = "a new string in the field"

        _odb.enabled = 0
        _odb.replaying = 1
        _odb.currentTimestamp = 0

        self.assertEqual(1, h.int)
        self.assertEqual(None, h.object)
        self.assertEqual("this is a string", h.string)

        _odb.currentTimestamp += 1
        ref.int = 2
        self.assertEqual(ref.int, h.int)
        _odb.currentTimestamp += 1
        ref.object = h
        self.assertEqual(ref.object, h.object)
        _odb.currentTimestamp += 1
        self.assertEqual(h.field, h.string)
        _odb.currentTimestamp += 1
        self.assertEqual(h.field, "a new string in the field")

        _odb.replaying = 0


def test_main():
    test_support.verbose = 1
    test_support.run_unittest(InstanceTestCase)

if __name__ == '__main__':
    test_main()
