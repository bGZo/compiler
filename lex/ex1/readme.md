## Usage

```shell
lex ex1.l
cc lex.yy.c -ll
# -ll make unnessary to add main.c file
# garb the main routine the default routine out of libary to run ahead.
./a.out
```