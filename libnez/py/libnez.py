# ASTMachine and Elastic Memoization
#
# Python version 3.2.5
#
# Author:
#   Shun Honda

from math import log
import sys

class CommonTree:
    def __init__(self, start, source):
        self.start = start
        self.end = start
        self.tag = None
        self.value = None
        self.child = []
        self.source = source

    def dump(self):
        self.stringfy('')

    def stringfy(self, indent):
        print(indent, end='')
        if(len(self.child) == 0):
            print('#' + self.tag + '[\'', end='')
            for char in self.source[self.start:self.end]:
                print(char, end='')
            print('\']')
        else:
            indent += '  '
            print('#' + self.tag + '[\'')
            for node in self.child:
                if(node is not None):
                    node.stringfy(indent)
                else:
                    print(indent, '{ None }')
            indent = indent[:-2]
            print(indent + ']')

class Module:
    def __init__(self):
        self.list = []

    def setFunction(self, func):
        self.list.append(func)

class Function:
    def __init__(self):
        self.list = []

    def setOperation(self, inst):
        self.list.append(inst)

class Instruction:
    Inew = 0
    Icapture = 1
    Ileftnew = 2
    Ileftcapture = 3
    Ilink = 4
    Itag = 5
    Ireplace = 6
    Icall = 7
    Iret = 8

    def __init__(self, inst, pos, value):
        self.inst = inst
        self.pos = pos
        self.value = value

class StackEntry:
    def __init__(self, linkMap, childSize):
        self.linkMap = linkMap
        self.childSize = childSize

class ASTMachineCompiler:
    def __init__(self):
        self.func = Function()
        self.callStack = [self.func]
        self.top = 0;
        self.unusedFunc = None

    def encode(self, inst, pos, value):
        operation = None
        if(inst == Instruction.Icall or inst == Instruction.Ileftnew):
            callFunc = Function()
            operation = Instruction(inst, pos, callFunc)
            self.func.setOperation(operation)
            self.callStack.append(self.func)
            self.top += 1
            self.func = callFunc
        elif(inst == Instruction.Iret or inst == Instruction.Ileftcapture):
            ret = self.callStack.pop()
            self.top -= 1
            operation = Instruction(inst, pos, ret)
            self.func.setOperation(operation)
            self.func = ret
        else:
            operation = Instruction(inst, pos, value)
            self.func.setOperation(operation)
        return operation

    def abort(self):
        self.unusedFunc = self.func
        self.func = self.callStack.pop()
        self.top -= 1
        self.func.list.pop()

    def abortFunc(self, index):
        del self.func.list[index:]


class ASTMachine:
    def __init__(self, inputs):
        self.inputs = inputs
        self.astStack = []
        self.astStackTop = -1
        self.callStack = [StackEntry({}, 0)]
        self.callStackTop = 0
        self.inst = {
                      Instruction.Inew:self.new,
                      Instruction.Icapture:self.capture,
                      Instruction.Ileftnew:self.leftnew,
                      Instruction.Ileftcapture:self.leftcapture,
                      Instruction.Ilink:self.link,
                      Instruction.Itag:self.tag,
                      Instruction.Ireplace:self.replace,
                      Instruction.Icall: self.call,
                      Instruction.Iret: self.ret
                    }

    def commitLog(self, func):
        if(len(func.list) is not 0):
            for operation in func.list[0:]:
                self.inst[operation.inst](operation.pos, operation.value)
            return self.astStack[self.astStackTop]

    def commitNode(self, childSize):
        if(not childSize == 0):
            parent = self.astStack[self.astStackTop]
            parent.child = [None] * childSize
            linkMap = self.callStack[self.callStackTop].linkMap
            for key in linkMap.keys():
                parent.child[key] = linkMap[key]


    def new(self, pos, value):
        # print('new')
        self.astStack.append(CommonTree(pos, self.inputs))
        self.astStackTop += 1

    def capture(self, pos, value):
        # print('capture')
        self.astStack[self.astStackTop].end = pos

    def leftnew(self, pos, value):
        # print('leftnew')
        self.commitNode(self.callStack[self.callStackTop].childSize) #ASTStackTop = left
        self.callStack.append(StackEntry({}, 1))
        self.callStackTop += 1
        self.callStack[self.callStackTop].linkMap[0] = self.astStack.pop()
        self.astStack.append(CommonTree(pos, self.inputs)) #ASTStackTop = merge
        self.commitLog(value)

    def leftcapture(self, pos, value):
        # print('leftcapture')
        self.commitNode(self.callStack[self.callStackTop].childSize)
        self.callStack.pop()
        self.callStackTop -= 1
        self.astStack[self.astStackTop].end = pos

    def link(self, pos, value):
        # print('link')
        self.astStackTop -= 1
        if(value == -1):
            self.callStack[self.callStackTop].linkMap[self.callStack[self.callStackTop].childSize] = self.astStack.pop()
            self.callStack[self.callStackTop].childSize += 1
        else:
            self.callStack[self.callStackTop].linkMap[value] = self.astStack.pop()
            if(value >= self.callStack[self.callStackTop].childSize):
                self.callStack[self.callStackTop].childSize = value + 1

    def tag(self, pos, value):
        # print('tag', '#'+value)
        self.astStack[self.astStackTop].tag = value

    def replace(self, pos, value):
        # print('replace')
        self.astStack[self.astStackTop].value = value

    def call(self, pos, value):
        # print('call')
        self.callStack.append(StackEntry({}, 0))
        self.callStackTop += 1
        self.commitLog(value)

    def ret(self, pos, value):
        # print('ret')
        self.commitNode(self.callStack[self.callStackTop].childSize)
        self.callStack.pop()
        self.callStackTop -= 1

class MemoEntry:
    def __init__(self):
        self.failed = False
        self.consumed = 0
        self.inst = None
        self.stateValue = 0
        self.key = -1

class ElasticTable:
    def __init__(self, w, n):
        self.memoArray = [None] * (w * n + 1)
        for i in range(len(self.memoArray)):
            self.memoArray[i] = MemoEntry()
        self.shift = int(log(n) / log(2.0)) + 1
        self.stat = MemoStat()

    def longkey(self, pos, memoPoint, shift):
        return ((pos << shift) | memoPoint) & sys.maxsize

    def setMemo(self, pos, memoPoint, failed, inst, consumed):
        key = self.longkey(pos, memoPoint, self.shift)
        hash = int(key % len(self.memoArray))
        m = self.memoArray[hash]
        m.key = key
        m.failed = failed
        m.inst = inst
        m.consumed = consumed
        self.stat.Stored += 1

    def getMemo(self, pos, memoPoint):
        key = self.longkey(pos, memoPoint, self.shift)
        hash = int(key % len(self.memoArray))
        m = self.memoArray[hash]
        if(m.key == key):
            self.stat.Used += 1
            return m
        return None

class MemoStat:
    def __init__(self):
        self.Stored = 0
        self.Used = 0
        self.Missed = 0

# compiler = ASTMachineCompiler()
# compiler.encode(Instruction.Inew, 0, 0)
# compiler.encode(Instruction.Icall, 0, None)
# compiler.encode(Instruction.Inew, 0, None)
# compiler.encode(Instruction.Itag, 0, 'Obj')
# compiler.encode(Instruction.Icapture, 3, None)
# compiler.encode(Instruction.Iret, 0, None)
# compiler.encode(Instruction.Ilink, 0, -1)
# compiler.encode(Instruction.Itag, 0, 'Source')
# compiler.encode(Instruction.Icapture, 0, None)
# compiler.encode(Instruction.Iret, 0, None)

#
# compiler = ASTMachineCompiler()
# compiler.encode(Instruction.Inew, 0, None)
# compiler.encode(Instruction.Itag, 0, 'Array')
# compiler.encode(Instruction.Icapture, 3, None)
# compiler.encode(Instruction.Ileftnew, 0, 0)
# compiler.encode(Instruction.Icall, 0, None)
# compiler.encode(Instruction.Inew, 0, None)
# compiler.encode(Instruction.Itag, 0, 'Obj')
# compiler.encode(Instruction.Icapture, 3, None)
# compiler.encode(Instruction.Iret, 0, None)
# compiler.encode(Instruction.Ilink, 0, -1)
# compiler.encode(Instruction.Itag, 0, 'Source')
# compiler.encode(Instruction.Ileftcapture, 0, None)
# compiler.encode(Instruction.Iret, 0, None)
#
# machine = ASTMachine()
# ast = machine.commitLog(compiler.func)
# ast.dump()

# compiler = ASTMachineCompiler()
# inst = compiler.encode(Instruction.Icall, 0, None)
#
# memoTable = ElasticTable(32, 4)
# memoTable.setMemo(134, 2, False, inst, 3)
# memoTable.setMemo(137, 3, False, inst, 3)
# m = memoTable.getMemo(134, 2)
# print(m.consumed)
# print(m.inst)
# print(m.failed)
#
# compiler.func.setOperation(inst)
# print(compiler.func.list.pop())
