class C():

    field = "a field"

    def __init__(self):
        self.int = 1
        self.object = None
        self.string = "this is a string"


c = C()
c.int = 2
c.object = c
c.string = c.field
c.field = "a new string in the field"