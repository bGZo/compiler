%{
    #include<stdio.h>
    int col=1, line=1;
    void print_out(char type);
%}


digit           [0-9]
letter          [a-zA-Z]

char            (\'[^ \n\r\t]\')
string          (\".{2,}\")

identifier      {letter}({letter}|{digit})*
integer         {digit}{digit}*

whitespace      [ ]
newline         [\n]
return_         [\r]
col_8           [\t]

illegal         ([^a-zA-Z1-9\t\n>=<:\'\(\)\;\[\]\.\!\+\-\/\*\"\,]+\{\})


%%
if|else|while|scanf|printf|int|void|return {print_out('R');col = col+yyleng;}

{newline}       {line += 1;         col = 1;}               /*换行*/

{whitespace}    {print_out('T');    col = col + yyleng;}    /*空格*/
{return_}       {print_out('T');    col = 1;}               /*返回当前行第一列*/
{col_8}         {print_out('T');    col += 8;}              /*制表符列加8*/

{integer} {
    if(yyleng > 10){
        printf("Error! The length of this number is too long!\n");
        exit(0);
    }
    print_out('C');                 col = col + yyleng;
}
{char} {
    if(yyleng > 3){
        printf("The length of char must be 3.");
        exit(0);
    }
    print_out('C');                 col = col+yyleng;
}
{string} {
    print_out('C');                 col = col + yyleng;
}

{identifier} {
    if(yyleng > 10){
        printf("Error! The length of this idetifier is too long!\n");
        exit(0);
    }
    print_out('I');                 col = col + yyleng;
}

"+"|"-"|"*"|"/"|"<"|">"|"="|"!="|">="|"<="  {print_out('O');col = col + yyleng;}

","|"("|")"|";"|"{"|"}"                     {print_out('B');col = col + yyleng;}

{illegal} {
    printf("This word is incorrect! ");
    print_out('T');
}
%%


void print_out(char type){printf("%s : %c, (%d,%d)\n",yytext,type,line,col);}

int main(int argc,char** argv){
    FILE * fIn;
    switch(argc){
        case 1: /*打开缺省文件*/
            fIn=fopen("config.in","r");
            if(fIn==NULL){
                printf("default file is not found\n");
                exit(1);
            }
            else yyin=fIn;
            break;
        case 2: /*打开指定文件*/
            if ((fIn = fopen(argv[1],"r")) == NULL) {
                    printf("File %s is not found.\n",argv[1]);
                    exit(1);
                }
                else yyin=fIn;
            break;
        default:
            printf("useage:flex [filename]\n");
            exit(1);
    }
    yylex();

    fclose(fIn);
    return 0;
}


/*C: constant,          常数(整数/字符/字符串)*/
/*S: separator,         分隔符(逗号/分号/括号(引号))*/
/*R: reserved words,    保留字*/
/*I: identifier,        标识符*/
/*B: bounds,            界符*/
/*O: operate,           算符*/
/*T: other,             其他*/
