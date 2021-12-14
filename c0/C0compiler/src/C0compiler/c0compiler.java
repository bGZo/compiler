package C0compiler;

import java.io.*;
import java.util.*;

enum fct{                       //虚拟机指令代码段
    LIT, RET, LOD, STO, CAL, INT, JMP, JPC, ADD, DIV, MUL, SUB, WRT, RED
}

enum object{                    //名字表中的类型
    integer,                    //整数型
    int_function,               //有返回函数
    void_function               //无返回函数
}

enum symbol{                    //标识符类型，符号值
    nul,ident,number,plus,minus,times,slash,
    lparen,rparen,becomes,comma,semicolon,lbrace,
    rbrace,elsesym,ifsym,intsym,printfsym,returnsym,
    scanfsym,voidsym,whilesym,mainsym
}

class instruct {                //虚拟机指令结构
    public fct f;
    public int l;
    public int a;
}

class tablestruct{
    String name;                //名字
    object kind;                //类型
    int val;                    //值
    int level;                  //层次
    int adr;                    //地址
    int size;                   //大小
}

class Ptx{
    int value;
    Ptx(){
        value=0;
    }
}

class Pdx{
    int value;
    Pdx(){
        value=0;
    }
}

public class c0compiler {

    public static String C0File = ".\\data\\test1";
    public static char [] line = new char[5000];        //C0文件总字符
    public static int len = 0;                          //C0文件总字符长度
    public static int cc = 0;                           //C0文件总字符指针
    public static char ch = ' ';                        //取出单个字符
    public static List<String> word = new ArrayList();  //9个保留字
    public static symbol wsym[] = new symbol[9];        //9个保留字对应的符号值
    public static String id = null;
    public static symbol sym = null;                    //当前的符号
    public static int num;                              //当前的number
    public static int cxmax = 200;                      //最多的虚拟机指令代码数
    public static instruct []code = new instruct[cxmax];//存放虚拟机代码的数组
    public static int cx = 0;                           //存放虚拟机代码的指针，取值范围为[0,cxmax-1]
    public static int txmax = 100;                      //名字表大小
    public static tablestruct []table = new tablestruct[txmax];
    public static int table_num = 1;
    public static int lev = 0;
    public static int cx0 = 1;                          //cx0保存该层时的虚拟机代码指令下标
    public static int funl = 0;                         //函数内局部变量个数
    public static Ptx tx = new Ptx();                   //名字表尾指针
    public static Pdx dx = new Pdx();                   //该层下相对位置
    public static int enter_main = 1;
    public static int tx0 = 0;                          //tx0保存函数所处位置以便回填size

    public static void main(String[] args) throws IOException {
        initFile();
        init();
        System.out.println(line);
        getsym();
        block();
        test();
        outPutFile();
    }

    public static void initFile() {
        System.out.println("C0源程序路径:");
        Scanner input=new Scanner(System.in);
        C0File=input.next();
    }
    public static void outPutFile() {
        System.out.println("符号表和中间代码保存路径:");
        Scanner input=new Scanner(System.in);
        String outC0Path=input.next();
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(outC0Path+"\\table.txt"));

            out.write("名字\t类型\t值\t层数\t地址\t大小\n");

            for(int i=1;i<=tx.value;i++)
                out.write(table[i].name+"\t"+table[i].kind+"\t"+table[i].val+"\t"+ table[i].level+"\t"+table[i].adr+"\t"+table[i].size+"\n");
            out.close();

            System.out.println("table文件创建成功!");
        } catch (IOException e) {
            System.out.println("打印 table 出错!");
        }

        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(outC0Path+"\\middleProgram.txt"));

            for(int i=0;i<cx;i++)
                out.write(code[i].f+" "+code[i].l+" "+code[i].a+"\n");
            out.close();

            System.out.println("middleProgram 创建成功!");
        } catch (IOException e) {
            System.out.println("middleProgram 创建失败!");
        }
    }

    public static int block() {
        /*
        * block() => 拦截词法匹配得到的 token 来语法匹配
        * dx    该层下相对位置
        * tx    名字表尾指针
        * tx0   保存函数所处位置 -> 回填size
        * lev   层
        * cx    虚拟代码尾指针
        * cx0   记录该层虚拟代码，用以记录函数返回值等
        * funl  函数内局部变量个数
        * */

        cx0 = cx;                                                           //始化数据的回填

        gen(fct.INT,0,4);
        gen(fct.JMP,0,enter_main);                                       // enter_main => main 位置(1)
        enter_main = cx-1;

        dx.value = 4;                                                       //初始化层数 => 隐式单元(0,1,2) + return

        while(sym == symbol.voidsym||sym == symbol.intsym) {
            if(sym == symbol.intsym) {                                      // int => 全局变量/函数
                getsym();
                if(sym==symbol.ident) {
                    getsym();
                    if(sym==symbol.comma||sym==symbol.semicolon){          //全局变量开始
                        code[cx0].a++;
                        enter(object.integer,tx,lev,dx);                   //更新符号表
                        do {
                            while(sym==symbol.comma) {                     //, => 多变量声明
                                getsym();
                                if(sym==symbol.ident){
                                    code[cx0].a++;                        //此时的栈INT指令每生成一个变量便+1
                                    enter(object.integer,tx,lev,dx);      //更新符号表
                                    getsym();
                                }
                                else error(4);
                            }
                            if(sym==symbol.semicolon){                    //; => 全局变量声明结束
                                getsym();
                                break;
                            }
                            else error(5);
                        }while(sym==symbol.ident);
                    }
                    else if(sym==symbol.lparen){                          // (   => 函数
                        if(lev==0) lev = 2;                               // 自定义函数在第2层
                        else lev++;
                        dx.value = 3;                                     // 初始化，每层拥有3个隐式单元
                        enter(object.int_function,tx,lev,dx);
                        tx0 = tx.value;
                        gen(fct.JMP,0,cx+1);
                        cx0 = cx;
                        gen(fct.INT,0,3);
                        funl = 0;

                        getsym();
                        if(sym!=symbol.rparen) error(6);              // 缺 )
                        else getsym();

                        if(sym!=symbol.lbrace) error(7);              // 缺 {
                        else {                                           // 自定义函数
                            getsym();
                            statement(tx,lev,dx);                        // statement结束，无类型的此时的sym应该是 }

                            if(sym!=symbol.returnsym) error(16);      // 返回的函数类型最后一个返回回的应该是return
                            else {
                                getsym();
                                expression(tx,lev,dx);                   // 表达式处理最后 return 语句
                            }

                            gen(fct.STO,0,0);

                            if(sym!=symbol.semicolon)error(13);
                            else getsym();

                            if(sym!=symbol.rbrace) error(12);
                            else getsym();

                            gen(fct.RET,0,0);
                        }
                    }
                }
            }                          // int => 全局变量/函数 结束
            else if(sym == symbol.voidsym) {
                getsym();

                if(sym == symbol.ident || sym == symbol.mainsym) {      // 自定义 void 类型的函数 / 主函数
                    if(sym==symbol.mainsym) {
                        code[enter_main].a = cx;                        // 更新主函数入口
                        gen(fct.CAL,0,cx+1);
                        lev=1;
                    } else {
                        if(lev==0)lev=2;
                        else lev++;
                    }

                    getsym();

                    if(sym==symbol.lparen) {                            // '('
                        dx.value = 3;
                        enter(object.void_function,tx,lev,dx);

                        tx0 = tx.value;
                        gen(fct.JMP,0,cx+1);

                        cx0 = cx;
                        gen(fct.INT,0,3);
                        funl = 0;
                        getsym();

                        if(sym!=symbol.rparen) error(6);             // ')'
                        else getsym();

                        if(sym!=symbol.lbrace) error(7);             // '{'
                        else {
                            getsym();

                            statement(tx,lev,dx);

                            if(sym==symbol.rbrace) getsym();           // '}'
                            else error(12);

                            gen(fct.RET,0,0);
                        }
                    }
                    else error(11);
                }
            }
        }
        return 1;
    }

    public static void statement(Ptx tx,int lev,Pdx dx) {
        /*
        * statement(): 处理语句
        * */
        while(sym != symbol.rbrace){                                  // 遇到'}' => 块处理 over
            if(sym == symbol.intsym){                                 // 变量声明 (int)
                getsym();
                if(sym==symbol.ident){                                // int + 标识符
                    getsym();
                    if(sym==symbol.comma||sym==symbol.semicolon){     // 是否声明结束
                        code[cx0].a++;                                // 更新虚拟指令
                        enter(object.integer,tx,lev,dx);
                        funl++;
                        do {
                            while(sym==symbol.comma) {                // ',' => 后跟多变量声明
                                getsym();
                                if(sym==symbol.ident){                // 存在多个变量声明
                                    code[cx0].a++;                    // 此时的栈INT指令每生成一个变量便+1
                                    enter(object.integer,tx,lev,dx);
                                    funl++;                           // 更新局部变量
                                    getsym();
                                }
                                else error(4);
                            }
                            if(sym==symbol.semicolon){                // ';' => 声明变量结束，跳出变量声明循环
                                getsym();
                                break;
                            }
                            else error(5);
                        }while(sym==symbol.ident);
                    }
                    else error(8);
                }
                else error(9);
                table[tx0].size=funl;
            }                             // 变量声明 (int) 结束
            else if(sym == symbol.ident) {                            // 遇到标识符 => 语句/函数调用
                int i = position(id, tx.value);                       // 查表
                if(i == 0){                                           // 变量未找到
                    error(10);
                    getsym();
                }
                else if(table[i].kind == object.int_function) call_intfunction(i);
                else if(table[i].kind == object.void_function) call_voidfunction(i);
                else if(table[i].kind == object.integer) assignment(tx,lev,dx);
            }                        // 遇到标识符结束
            // if\while\scanf 结束已取到下一个 sym
            else if(sym==symbol.ifsym) condition(tx,lev,dx);          // 条件语句
            else if(sym==symbol.whilesym) loop(tx,lev,dx);            // 循环语句
            else if(sym==symbol.scanfsym) read(tx,lev,dx);            // scanf 语句
            else if(sym==symbol.printfsym) write(tx,lev,dx);          // printf 语句
            else if(sym==symbol.returnsym) break;                     // return 语句(最后)
            else {
                System.out.println(sym);
                error(2);
            } //无法识别
        }
    }
    public static void write(Ptx tx,int lev,Pdx dx){
        /*
        * write: 写语句
        *   <写语句>->printf '(' [ <表达式>] ')' ;
        * */
        getsym();

        if(sym==symbol.lparen){                                       // '('
            getsym();

            expression(tx,lev,dx);

            if(sym==symbol.rparen) getsym();
            else error(6);

            if(sym==symbol.semicolon)getsym();
            else error(13);

            gen(fct.WRT,0,0);
        }
        else error(11);
    }

    public static void read(Ptx tx,int lev,Pdx dx) {
        /*
        * scanf()
        *   <读语句> -> scanf '(' id ')';
        * */
        getsym();
        if(sym != symbol.lparen) error(11);
        else getsym();

        int i=position(id,tx.value);
        if(i==0) error(10);
        else {
            gen(fct.RED,0,0);
            gen(fct.STO,lev-table[i].level,table[i].adr);
        }

        getsym();
        if(sym!=symbol.rparen)error(6);
        else getsym();

        if(sym!=symbol.semicolon)error(13);
        getsym();
    }

    public static void loop(Ptx tx,int lev,Pdx dx) {
        /*
        * loop(): 循环语句语法处理
        *   <循环语句> -> while '(' <表达式> ')' <语句>
        * */
        int cx1,cx2;
        getsym();

        if(sym==symbol.lparen){                                         // '('
            getsym();

            cx1 = cx;                                                   // 保存条件判断位置
            expression(tx,lev,dx);

            cx2 = cx;                                                   // 保存循环体的结束的下一位置
            gen(fct.JPC,0,0);

            if(sym==symbol.rparen){                                      // ')'
                getsym();

                if(sym==symbol.lbrace) getsym();                         // '{'
                else error(7);

                statement(tx,lev,dx);

                gen(fct.JMP,0, cx1);                                // 反填起始位置, 重新判断条件
                code[cx2].a=cx;                                         // 反填跳出循环的地址，类if
            }
            else error(6);

            if(sym==symbol.rbrace) getsym();                            // '}'
            else error(12);
        }
        else error(11);
    }

    public static void condition(Ptx tx,int lev,Pdx dx) {
        /*
        * condition(): 条件语句语法识别
        *  cx1 => if 起始位置
        *  cx2 => else 起始位置
        *  cx3 => if 结束位置
        * */
        int cx1, cx2, cx3;
        getsym();

        if(sym==symbol.lparen){                                         // '('
            getsym();
            expression(tx,lev,dx);                                      // 处理表达式
            if(sym == symbol.rparen){                                   // ')'
                getsym();

                cx1 = cx;                                              // 保存当前指令地址 (if起始地址)
                gen(fct.JPC,0,0);                               // 跳转指令地址暂写占位 0.

                if(sym == symbol.lbrace) getsym();                     // '{'
                else error(7);

                statement(tx,lev,dx);

                cx3 = cx;                                              // 保存 if 语句结束位置
                if(sym == symbol.rbrace) getsym();                     // '}'
                else error(12);

                if(sym == symbol.elsesym) {
                    gen(fct.JMP,0,0);
                    getsym();

                    cx2 = cx;
                    if(sym == symbol.lbrace) getsym();                 // '{'
                    else error(7);

                    statement(tx,lev,dx);

                    if(sym==symbol.rbrace) getsym();                   // '}'
                    else error(12);
                    /*
                    * cx  => 语句执行完的位置
                    * cx2 => else语句的起始地址
                    * */
                    code[cx1].a=cx2;                                   // if 后跳转 else语句
                    code[cx3].a=cx;                                    // else 执行后结束
                }
                else code[cx1].a=cx;
            }
            else error(6);
        }
        else error(11);
    }

    public static void assignment(Ptx tx,int lev,Pdx dx) {
        /*
        * assignment(): 赋值处理
        *   <赋值语句> -> id = <表达式> ;
        * */

        if(sym==symbol.ident){
            int i=position(id,tx.value);
            if(i==0)error(10);

            getsym();

            if(sym==symbol.becomes){
                getsym();

                expression(tx,lev,dx);
                /*
                * 依照解释器设计, STO 第二操作数为相对层差
                * */
                gen(fct.STO,lev-table[i].level,table[i].adr);
            } else{
                error(14);
                getsym();
            }

            if(sym!=symbol.semicolon) error(13);
            else getsym();
        }
    }

    public static void expression(Ptx tx,int lev,Pdx dx) {
        /*
        * [已取出 sym ]
        * expression(): 语法分析表达式的判断
        *   <表达式> -> [+|-] <项> {(+|-) <项> }
        * */

        symbol addop;                                                   // 正负号
        if(sym != symbol.plus && sym != symbol.minus) term(tx,lev,dx);

        while(sym==symbol.plus||sym==symbol.minus) {                    // 重复处理多项表达式
            addop=sym;
            getsym();
            term(tx,lev,dx);
            if(addop==symbol.plus) gen(fct.ADD,0,0);
            else if(addop==symbol.minus) gen(fct.SUB,0,0);
            else {
                System.out.println(sym);
                error(2);
            }
        }

    }

    public static void term(Ptx tx,int lev,Pdx dx) {
        /*
        * [已读入 sym ]
        * term(): 判断项
        *   <项> -> <因子> { (*|/) 因子}
        * */
        symbol mulop;                                                       // 保存乘除号
        if(sym != symbol.times && sym != symbol.slash) factor(tx,lev,dx);   // 处理因子

        while(sym == symbol.times || sym == symbol.slash) {                 // 重复处理多因子项
            mulop = sym;
            getsym();
            factor(tx,lev,dx);
            if(mulop == symbol.times) gen(fct.MUL,0,0);
            else if(mulop == symbol.slash) gen(fct.DIV,0,0);
            else {
                System.out.println(sym);
                error(2);
            }
        }

    }

    public static void factor(Ptx tx,int lev,Pdx dx) {
        /*
        * [已读入 sym ]
        * factor(): 处理因子;
        *   <因子> -> id | '('表达式')' | num | <自定义函数调用>
        * */
        if(sym == symbol.ident){                                            // 标识符 => 整型/函数调用
            int i = position(id,tx.value);
            if(i == 0) error(10);
            else {
                switch(table[i].kind) {
                    case integer:
                        gen(fct.LOD,lev-table[i].level,table[i].adr);
                        getsym();
                        break;
                    case int_function:
                        call_intfunction(i);
                        gen(fct.LOD,0,0);
                        break;
                }
            }
        }                                       // 标识符 => 整型/函数调用 结束
        else if(sym==symbol.number) {                                      // 数字
            gen(fct.LIT,0,num);
            getsym();
        }                                  // 数字 结束
        else if(sym==symbol.lparen) {                                      // 表达式
            getsym();
            expression(tx,lev,dx);
            if(sym==symbol.rparen)
                getsym();
            else
                error(6);
        }                                  // 表达式 结束
        else error(15);
    }
    public static void call_intfunction(int i) {
        getsym();

        if(sym==symbol.lparen) getsym();
        else error(11);
        if(sym==symbol.rparen) getsym();
        else error(6);
        /*
        * FIXME: get a semi, then parsing will add a more step.
        * //???
        * */
        gen(fct.CAL,0,table[i].adr);
        //得到了下一个sym，即;后的下一个词，当a();}时，已经得到了}
    }
    public static void call_voidfunction(int i) {
        getsym();

        if(sym==symbol.lparen)getsym();
        else error(11);
        if(sym==symbol.rparen)getsym();
        else error(6);
        if(sym==symbol.semicolon)getsym();
        else error(13);

        gen(fct.CAL,0,table[i].adr);
        //得到了下一个sym，即;后的下一个词，当a();}时，已经得到了}
    }

    public static void test() {
        System.out.println("############## 最终结果 ###################");
        outTable();
        outInstruct();
        System.out.println("[tx] 名字表项目:" + (tx.value + 1));
        System.out.println("[cx] 虚拟代码数:" + (cx-1) );
        System.out.println("[dx] 最大层数:" + dx.value );
//        System.out.println("[lev] 此时层差:" + lev); //main() 通常为 1
    }

    public static void enter(object k,Ptx ptx,int lev,Pdx pdx) {    //写入符号表
        ptx.value++;                                                //更新名字表数
        table_num++;

        table[ptx.value].name=id;
        table[ptx.value].kind=k;

        switch(k){
            case integer:                                           //对于整数，记录名字、类型、层以及相对位置，相对位置为该层的相对位置
                table[ptx.value].level = lev;
                table[ptx.value].adr = pdx.value;
                pdx.value++;                                        //该层下相对位置(层数+1)
                break;
            case int_function:                                      //对于函数，记录名字、类型和层
                table[ptx.value].level = lev;
                table[ptx.value].adr = cx;
                break;
            case void_function:
                table[ptx.value].level = lev;
                table[ptx.value].adr = cx;
                break;
        }
    }

    public static void outTable() {
        System.out.println("---------------名字表---------------------");
        System.out.println("名字\t类型\t值\t层数\t地址\t大小");
        for(int i=0;i<table_num;i++)
            System.out.println(table[i].name+"\t"+table[i].kind+"\t"+table[i].val+"\t"+table[i].level+"\t"+table[i].adr+"\t"+table[i].size+"\t");
    }

    public static int position(String idt,int tx) {
        int i=tx;
        table[0].name=idt;

        while(table[i].name.equals(idt) == false) i--;
        /*NOTE:
        * 千万注意传进来的是一个最大值table_num的值减一，不然会报空指针错
        * */
        return i;
    }

    public static void outInstruct() {
        System.out.println("---------------栈------------------------");
        for(int i=0;i<cx;i++) System.out.println(i+" "+code[i].f+" "+code[i].l+" "+code[i].a);
    }

    public static int gen(fct x,int y,int z) {
        /*
        * gen(): 添加生成(x,y,z)指令
        * */
        if(cx>=cxmax) {
            error(3);
            return -1;
        }

        code[cx].f=x;
        code[cx].l=y;
        code[cx].a=z;
        cx++;

        return 1;
    }

    public static void init() {
        initC0line(C0File);
        //对保留字进行初始化

        word.add("int");
        word.add("void");
        word.add("main");
        word.add("if");
        word.add("else");
        word.add("while");
        word.add("return");
        word.add("scanf");
        word.add("printf");

        wsym[0]=symbol.intsym;
        wsym[1]=symbol.voidsym;
        wsym[2]=symbol.mainsym;
        wsym[3]=symbol.ifsym;
        wsym[4]=symbol.elsesym;
        wsym[5]=symbol.whilesym;
        wsym[6]=symbol.returnsym;
        wsym[7]=symbol.scanfsym;
        wsym[8]=symbol.printfsym;


        for(int i=0;i<cxmax;i++) code[i]=new instruct();        //实例化虚拟机代码指令数组
        for(int i=0;i<txmax;i++) table[i]=new tablestruct();    //实例化名字表数组
    }

    public static symbol ssym(char ch2) {                       //单字符的符号表
        if(ch2=='=') return symbol.becomes;
        if(ch2=='+') return symbol.plus;
        if(ch2=='-') return symbol.minus;
        if(ch2=='*') return symbol.times;
        if(ch2=='/') return symbol.slash;
        if(ch2==',') return symbol.comma;
        if(ch2==';') return symbol.semicolon;
        if(ch2=='(') return symbol.lparen;
        if(ch2==')') return symbol.rparen;
        if(ch2=='{') return symbol.lbrace;
        if(ch2=='}') return symbol.rbrace;
        return null;
    }

    public static void initC0line(String C0File) {              // C0文件全部读入 line 中
        try {
            Reader read=new FileReader(new File(C0File));
            int temp;
            while((temp=read.read())!=-1) {                     // 接受每一个内容
                line[len]=(char)temp;
                len++;
            }
            read.close();
        } catch (Exception e) {
            error(1);
            e.printStackTrace();
        }
    }

    public static void initC0lineTest() {
        for(int i=0;i<len;i++) {
            if(line[i]=='\n') { System.out.print("N");continue; }
            if(line[i]=='\r') { System.out.print("R");continue; }
            System.out.print(line[i]);
        }
        System.out.println(len);
    }


    public static int getch() {
        /*
        * getch(): getsym调用, 吃掉一个字符
        * */
        ch=line[cc];
        cc++;
        return 0;
    }

    public static int getsym() {
        /*
        * 词法分析，获取一个符号
        * */
        while(ch == '\n'||ch == '\r'||ch == '\t'||ch == ' ') getch();

        if(ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z'||ch == '_'){    // 名字/保留字
            StringBuffer k=new StringBuffer();
            do {                                                            // 拿到一个 token 的单词量
                k.append(ch);
                getch();
            }while(ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch == '_' || (ch >= '0' && ch <= '9'));
            id=k.toString();

            if(word.contains(id)) sym = wsym[word.indexOf(id)];             //搜索是否为保留字
            else sym=symbol.ident;

            System.out.println(sym+": "+id);
        } else{                                                             // 名字/保留字 不可能; 数字
            if(ch >= '0' && ch <= '9') {
                StringBuffer k=new StringBuffer();
                num = 0;
                sym = symbol.number;
                do {
                    k.append(ch);
                    getch();
                }while(ch>='0'&&ch<='9');
                num=Integer.parseInt(k.toString());

                System.out.println(sym+" :"+num);
            } else{                                                         // 数字也不可能; 单字符
                sym=ssym(ch);

                if(sym!=null) System.out.println(sym);
                else{
                    System.out.println(sym);
                    error(2);
                }
                getch();
            }
        }
        return 0;
    }

    public static void error(int i) {
        switch(i) {
            case 1:  System.out.println("[E1] 文件无法打开."); break;
            case 2:  System.out.println("[E2] 标识符无法被识别.");break;
            case 3:  System.out.println("[E3] 程序过长, 无法生成指令, 符号表溢出.");break;
            case 4:  System.out.println("[E4] 在标识符的逗号之后应该是标识符.");break;
            case 5:  System.out.println("[E5] 标识符后面应该是分号.");break;
            case 6:  System.out.println("[E6] 缺失 ')'.");break;
            case 7:  System.out.println("[E7] 缺失 '{'.");break;
            case 8:  System.out.println("[E8] 此处应该是 ',' 或 ';'.");break;
            case 9:  System.out.println("[E9] 应该是标识符");break;
            case 10: System.out.println("[E10] 无法找到标识符");break;
            case 11: System.out.println("[E11] 缺失 '('.");break;
            case 12: System.out.println("[E12] 缺失 '}'.");break;
            case 13: System.out.println("[E13] 缺失 ';'.");break;
            case 14: System.out.println("[E14] 赋值语句缺少 '='.");break;
            case 15: System.out.println("[E15] 分析元素错误.");break;
            case 16: System.out.println("[E16] int_function 没有定义返回值.");break;
        }
    }

}
