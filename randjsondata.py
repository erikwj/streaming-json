import json
import os
from random import *
import string
import sys

# Copied json generator from github.com/non/jawn

constants = [True, False]

def mkconstant():
    return choice(constants)

def mkinteger():
    return randint(-1e3, 1e3) * (10 ** normalvariate(0, 4)) + randint(-1e3, 1e3)

def mksmallint():
    return randint(1000000, 2**22)

def mkshort():
    return randint(1,256)

def mkdouble():
    return random() * (10 ** normalvariate(0, 30))

def mknum():
    if randint(0, 1):
        return mkdouble()
    else:
        return mkinteger()

def mkstring():
    n = int(min(abs(normalvariate(40, 20)), abs(normalvariate(30, 10))))
    return ''.join([choice(string.ascii_letters) for i in range(0, n)])

values = [mkconstant, mknum, mknum, mknum, mkstring]

def mkvalue():
    return choice(values)()

if __name__ == "__main__":
    args = sys.argv[1:]
    try:
        num = int(args[0])
        path = args[1]
        print "writing json (%d rows) into %s" % (num, path)
        f = open(path, 'w')
        f.write("{\"testjsons\":[")
        for i in range(0, num):
            if i > 0: f.write(", ")
            c = {"name": mkstring(),
                 "average": mkdouble(),
                 "isValid": choice(constants),
                 "dimensions": {"height": mkshort(), "width": mkshort(), "length": mkshort()},
                 "owner": {"name": mkstring(), "bankaccount": mksmallint()},
                 "elements": [
                    {"label": mkstring(), "quantity": mkshort()},
                    {"label": mkstring(), "quantity": mkshort()},
                    {"label": mkstring(), "quantity": mkshort()}
                 ]}
            f.write(json.dumps(c))
        f.write("]}")
        f.close()
    except Exception, e:
        print "usage: %s NUM PATH" % sys.argv[0]
