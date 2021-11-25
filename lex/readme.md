
Refer to https://www.youtube.com/watch?v=54bo1qaHAfk

## What Lex

- a scanner generator
  - input is a set of Regex and associated actions(in c)
  - output is a table-driven scanner(lex.yy.c)
- flex: an open source implement of original Unix lex utility

## Usage

```shell
lex ex1.l
cc lex.yy.c -ll
# -ll make unnessary to add main.c file
# garb the main routine the default routine out of libary to run ahead.
./a.out
```



