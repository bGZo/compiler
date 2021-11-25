## Usage

```shell
lex myscanner.l
gcc myscanner.c lex.yy.c -o myscanner.out
cc myscanner.c lex.yy.c -o myscanner.out
./a.out < config.in
```