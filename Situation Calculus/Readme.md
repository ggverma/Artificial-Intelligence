# Monkey-Banana Problem

This is a situation calculus design of finding a solution in a world where rules are defined by First Order Logic.

## Problem Statement

The monkey-and-bananas problem is faced by a monkey in a laboratory with some bananas hanging out of reach from the ceiling. A box is available that will enable the monkey to reach the bananas if he climbs on it. Initially, the monkey is at posa, banana1 is at posa, banana2 is at posb, banana3 is at posc, and the box is at posd. The monkey and box have height low, but if the monkey climbs
onto the box he will have height high, the same as the bananas. The actions available to the monkey include go from one place to another, push the box from one place to another, climb up onto or climb down from the box, and grasp or ungrasp a banana. The result of a grasp is that the monkey holds the object if the monkey and object are in the same place at the same height, the result of an ungrasp is that the monkey does not hold the object any more if he currently holds that object, and the object will be left at the current place and the current height of the monkey. The monkey can holds multiple bananas at the same time, but he cannot push box if he holds any banana. The initial states are given as:

at(banana1, posa, high, init).
at(banana2, posb, high, init).
at(banana3, posc, high, init).
at(box, posd, low, init).
at(monkey, posa, low, init).
holds([], init).

The fluent at(O, P, H, S) means an object O locates at a position P having height H at situation S.

Fluent holds(L, S) means the monkey holds and only holds items in list L at situation S. Actions are defined as:
go(Pos) % go to position Pos
push(Pos) % push box to position Pos
climbup()
climbdown()
grasp(Obj)
ungrasp(Obj)

The initial situation is init. do(A, S) represents the new situation after applying an action A on situation S.
