package C0compiler;
import java.io.*;
import java.util.*;
//虚拟机指令代码段
enum fct{
    LIT, RET, LOD, STO, CAL, INT, JMP, JPC, ADD, DIV, MUL, SUB, WRT, RED
}
//虚拟机指令结构
class instruct {
    public fct f;
    public int l;
    public int a;
}
//名字表中的类型
enum object{
    integer,        //整数型
    int_function,    //有返回的函数
    void_function    //无返回的函数
}
class tablestruct{
    String name;            //名字
    object kind;            //类型
    int val;                //值
    int level;                //层次
    int adr;                //地址
    int size;                //大小
}
//标识符类型，符号值
enum symbol{
    nul,ident,number,plus,minus,times,slash,
    lparen,rparen,becomes,comma,semicolon,lbrace,
    rbrace,elsesym,ifsym,intsym,printfsym,returnsym,
    scanfsym,voidsym,whilesym,mainsym
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
public class C0compiler {
    public static String C0File="C:\\Users\\Chester\\Desktop\\C0compiler\\C05.txt";
    public static char [] line=new char[5000];    //C0文件总字符
    public static int len=0;    //C0文件总字符长度
    public static int cc=0;        //C0文件总字符指针
    public static char ch=' ';        //取出单个字符
    public static List<String> word =new ArrayList();//9个保留字
    public static symbol wsym[]=new symbol[9];        //9个保留字对应的符号值
    public static String id=null;
    public static symbol sym=null;            //当前的符号
    public static int num;                    //当前的number
    public static int cxmax=200;                            //最多的虚拟机指令代码数
    public static instruct [] code=new instruct[cxmax];;    //存放虚拟机代码的数组
    public static int cx=0;    //存放虚拟机代码的指针，取值范围为[0,cxmax-1]
    public static int txmax=100;        //名字表大小
    public static tablestruct [] table=new tablestruct[txmax];
    public static int table_num=1;
    public static int lev=0;
    public static int cx0=1;    //cx0保存该层时的虚拟机代码指令下标
    public static int funl=0;    //函数内局部变量个数
    public static Ptx tx=new Ptx();    //名字表尾指针
    public static Pdx dx=new Pdx();    //该层下相对位置
    public static int enter_main=1;
    public static int tx0=0;    //tx0保存函数所处位置以便回填size
    public static void main(String[] args) throws IOException {
        // TODO Auto-generated method stub
        initFile();
        init();
        System.out.println(line);
//        System.out.println(len);
//        do {
//            getsym();
//        }
//        while(cc<len);    
//        cc=0;
//        gen(fct.ADD,10,9);
//        outInstruct();
//        Ptx ptx=new Ptx();
//        id="a";
//        enter(object.integer,ptx,1,new Pdx());
//        id="b";
//        enter(object.void_function,ptx,10,new Pdx());
//        id="c";
//        enter(object.int_function,ptx,1,new Pdx());
//        outTable();
//        System.out.println("position:"+position("aa",table_num-1));;
//
//        gen(fct.CAL,1,1);
//        outInstruct();
        getsym();
        block();
        test();
        outPutFile();
    }
    public static void initFile() {
        System.out.println("请输入C0程序路径：");
        Scanner input=new Scanner(System.in);
        C0File=input.next();
    }
    public static void outPutFile() {
        System.out.println("请输入符号表和中间代码保存路径：");
        Scanner input=new Scanner(System.in);
        String outC0Path=input.next();
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(outC0Path+"\\C0Table.txt"));
            out.write("名字name\t类型kind\t值val\t层次level\t地址adr\t大小size\n");
            for(int i=1;i<=tx.value;i++) {
                out.write(table[i].name+"\t"+table[i].kind+"\t"+table[i].val+"\t"+table[i].level+"\t"+table[i].adr+"\t"+table[i].size+"\n");
            }
            out.close();
            System.out.println("C0Table文件创建成功！");
            }
        catch (IOException e) {
            System.out.println("Print Table error!");
        }
        
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(outC0Path+"\\C0MiddleProgram.txt"));
            for(int i=0;i<cx;i++) {
                out.write(code[i].f+" "+code[i].l+" "+code[i].a+"\n");
            }
            out.close();
            System.out.println("C0MiddleProgram文件创建成功！");
            }
        catch (IOException e) {
            System.out.println("Print MiddleProgram error!");
        }
    }
    public static int block() {
        //dx 该层下相对位置
        //tx 名字表尾指针
        //lev层
        //cx 虚拟代码尾指针
        cx0=cx;//cx0记录该层虚拟代码，用以记录函数返回值等，此处用以记录初始化数据的回填操作
        gen(fct.INT,0,4);
        gen(fct.JMP,0,enter_main);//enter_main用来记录main函数的位置，
        enter_main=cx-1;
        dx.value=4;
        //起始初始化 1单位隐藏单元用于return，3单元隐式单元 0,1,2,3
        
        while(sym==symbol.voidsym||sym==symbol.intsym) {
            if(sym==symbol.intsym){    //如果是int，则可能是全局变量或函数
                getsym();
                if(sym==symbol.ident) {
                    getsym();
                    if(sym==symbol.comma||sym==symbol.semicolon){//定义数据
                        code[cx0].a++;
                        enter(object.integer,tx,lev,dx);    //插入符号表当中
                        do {
                            while(sym==symbol.comma) {        //如果是','则后应跟多个变量声明
                                getsym();
                                if(sym==symbol.ident){        //后跟多个变量声明
                                    code[cx0].a++;            //此时的栈INT指令每生成一个变量便+1
                                    enter(object.integer,tx,lev,dx);    //每有一个变量便加入到符号表中
                                    getsym();
                                }
                                else error(4);
                            }
                            if(sym==symbol.semicolon){        //如果后跟';'则表示声明变量结束，跳出变量声明循环
                                getsym();
                                break;
                            }
                            else error(5);
                        }while(sym==symbol.ident);
                    }
                    else if(sym==symbol.lparen){//是函数
                        if(lev==0)lev=2;//如果是在第零层，则直接等于2，让出第一层给main
                        else lev++;    //否则，层数应累次递增
                        dx.value=3;    //初始化，每层起始相对位置为3，让出三个隐式单元
                        enter(object.int_function,tx,lev,dx);
                        tx0=tx.value;
                        gen(fct.JMP,0,cx+1);
                        cx0=cx;
                        gen(fct.INT,0,3);
                        funl=0;
                        
                        getsym();
                        if(sym!=symbol.rparen)error(6);//缺少右小括号
                        else getsym();
                        if(sym!=symbol.lbrace)error(7);    //缺少左大括号
                        else {
                            //此时的sym等于'{'，自定义函数部分开始：
                            getsym();
                            //得到'{'后下一个词
                            statement(tx,lev,dx);
                            //statement结束后，此时的sym应该是}
                            if(sym!=symbol.returnsym)error(16);//有返回的函数类型最后一个返回回的应该是return
                            else {
                                getsym();
                                expression(tx,lev,dx);//表达式处理最后的return语句
                            }
                            gen(fct.STO,0,0);
                            if(sym!=symbol.semicolon)error(13);
                            else getsym();
                            if(sym!=symbol.rbrace)error(12);
                            else
                                getsym();
                            gen(fct.RET,0,0);
                        }    
                    }
                }                    
            }
            else if(sym==symbol.voidsym) {
                getsym();
                if(sym==symbol.ident||sym==symbol.mainsym){//只能是自定义void类型的函数
                    if(sym==symbol.mainsym) {
                        code[enter_main].a=cx;
                        gen(fct.CAL,0,cx+1);
                        lev=1;
                    }
                    else {
                        if(lev==0)lev=2;
                        else lev++;
                    }
                    getsym();
                    if(sym==symbol.lparen) {
                        dx.value=3;
                        enter(object.void_function,tx,lev,dx);
                        tx0=tx.value;
                        gen(fct.JMP,0,cx+1);
                        cx0=cx;
                        gen(fct.INT,0,3);
                        funl=0;
                        
                        
                        getsym();
                        if(sym!=symbol.rparen)error(6);
                        else getsym();
                        if(sym!=symbol.lbrace)error(7);
                        else {
                            getsym();
                            statement(tx,lev,dx);
                            if(sym==symbol.rbrace)
                                getsym();
                            else
                                error(12);
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
        while(sym!=symbol.rbrace){//语句始终判断是不是右大括号结尾，如果不是则一直都是语句处理
            //变量声明
            if(sym==symbol.intsym){//如果读入一个词是int 则只能是变量声明
                getsym();
                if(sym==symbol.ident){//如果int后跟标识符
                    getsym();
                    if(sym==symbol.comma||sym==symbol.semicolon){//判断标识符是不是','或';'结尾
                        code[cx0].a++;
                        enter(object.integer,tx,lev,dx);
                        funl++;
                        do {
                            while(sym==symbol.comma) {        //如果是','则后应跟多个变量声明
                                getsym();
                                if(sym==symbol.ident){        //后跟多个变量声明
                                    code[cx0].a++;            //此时的栈INT指令每生成一个变量便+1
                                    enter(object.integer,tx,lev,dx);
                                    funl++;//每有一个变量便加入到符号表中
                                    getsym();
                                }
                                else error(4);
                            }
                            if(sym==symbol.semicolon){        //如果后跟';'则表示声明变量结束，跳出变量声明循环
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
            }
            //当前句子第一个单词是标识符，可能是语句或者函数调用
            else if(sym==symbol.ident) {
                int i=position(id,tx.value);
                if(i==0){//变量未找到
                    error(10);
                    getsym();
                }
                else if(table[i].kind==object.int_function)call_intfunction(i);
                else if(table[i].kind==object.void_function)call_voidfunction(i);
                else if(table[i].kind==object.integer)assignment(tx,lev,dx);//类型为integer 赋值语句 //此时的sym仍然是ident
            }
            else if(sym==symbol.ifsym)//准备按照条件语句处理    //if\while\scanf函数结束都已经得到了下一个sym
                condition(tx,lev,dx);
            else if(sym==symbol.whilesym)//循环语句
                loop(tx,lev,dx);
            else if(sym==symbol.scanfsym)
                read(tx,lev,dx);
            else if(sym==symbol.printfsym)
                write(tx,lev,dx);
            else if(sym==symbol.returnsym)break;//返回语句处理，最后一个是return;
            else error(2);//无法识别
        }
    }
    public static void write(Ptx tx,int lev,Pdx dx){

        getsym();
        if(sym==symbol.lparen){//(
            getsym();
            expression(tx,lev,dx);
            if(sym==symbol.rparen)
                getsym();
            else
                error(6);
            if(sym==symbol.semicolon)
                getsym();
            else
                error(13);
            gen(fct.WRT,0,0);
        }
        else error(11);
    }
    public static void read(Ptx tx,int lev,Pdx dx) {
        getsym();
        if(sym!=symbol.lparen)error(11);
        else getsym();
        int i=position(id,tx.value);
        if(i==0)error(10);
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
        int cx1,cx2;
        getsym();
        if(sym==symbol.lparen){//(
            getsym();
            cx1=cx;//保存判断条件操作的位置
            expression(tx,lev,dx);
            cx2=cx;//保存循环体的结束的下一位置
            gen(fct.JPC,0,0);
            if(sym==symbol.rparen){//)
                getsym();
                if(sym==symbol.lbrace)//{
                    getsym();
                else error(7);
                statement(tx,lev,dx);
                gen(fct.JMP,0,cx1);//反填，重新判断条件
                code[cx2].a=cx;//反填跳出循环的地址，与if类似
                
            }
            else error(6);
            if(sym==symbol.rbrace)//}
                getsym();
            else
                error(12);
        }
        else
            error(11);
    }
    public  static void condition(Ptx tx,int lev,Pdx dx) {
        //此时sym为ifsym
        int cx1,cx2,cx3;/*cx1用于保存if的起始位置，cx2用于保存else起始位置，cx3保存if结束位置；*/
        getsym();
        //得到if后一个sym
        if(sym==symbol.lparen){    //如果是'('
            getsym();
            expression(tx,lev,dx);
            if(sym==symbol.rparen){    //)
                getsym();
                cx1=cx;//保存当前指令地址，即if的起始地址
                gen(fct.JPC,0,0);    //生成条件跳转指令，跳转地址暂写0
                if(sym==symbol.lbrace)//{
                    getsym();
                else
                    error(7);
                statement(tx,lev,dx);
                cx3=cx;    //保存if语句结束位置
                if(sym==symbol.rbrace)//}
                    getsym();
                else
                    error(12);
                if(sym==symbol.elsesym) {
                    gen(fct.JMP,0,0);
                    getsym();
                    cx2=cx;
                    if(sym==symbol.lbrace)//{
                        getsym();
                    else
                        error(7);
                    statement(tx,lev,dx);
                    if(sym==symbol.rbrace)//}
                        getsym();
                    else
                        error(12);
                    code[cx1].a=cx2;/*cx2为else语句的起始地址，若有else语句，则正是前面未定的跳转地址*/
                    code[cx3].a=cx; /*cx为语句执行完的位置*/
                        
                }
                else code[cx1].a=cx;
            }
            else error(6);
        }
        else error(11);
            
    }
    public static void assignment(Ptx tx,int lev,Pdx dx) {
        if(sym==symbol.ident){//a
            int i=position(id,tx.value);
            if(i==0)error(10);
            getsym();
            if(sym==symbol.becomes){//=
                getsym();//已经取出=后面的一个词 sym
                expression(tx,lev,dx);
                gen(fct.STO,lev-table[i].level,table[i].adr);//由于解释器的设计问题，此处sto入的是相对层差，因此用lev减去外层的值
                
            }
            else{//赋值语句没有等于号，报错
                error(14);
                getsym();
            }
            if(sym!=symbol.semicolon)error(13);
            else getsym();
        }
    }
    public static void expression(Ptx tx,int lev,Pdx dx) {
        //此时已经取出表达式的第一个sym
        symbol addop;//保存正负号
        //<表达式>->[+|-]<项>{(+|-)<项>}
        if(sym!=symbol.plus&&sym!=symbol.minus)term(tx,lev,dx);
        while(sym==symbol.plus||sym==symbol.minus)
        {
            addop=sym;
            getsym();
            term(tx,lev,dx);
            if(addop==symbol.plus)
                gen(fct.ADD,0,0);
            else if(addop==symbol.minus)
                gen(fct.SUB,0,0);
            else error(2);
        }
        
    }
    public static void term(Ptx tx,int lev,Pdx dx) {
        //已经读入项的第一个sym
        //<项>-><因子>{(*|/)因子}
        symbol mulop;//保存乘除号
        if(sym!=symbol.times&&sym!=symbol.slash)
            factor(tx,lev,dx);
        while(sym==symbol.times||sym==symbol.slash) {
            mulop=sym;
            getsym();
            factor(tx,lev,dx);
            if(mulop==symbol.times)
                gen(fct.MUL,0,0);
            else if(mulop==symbol.slash)
                gen(fct.DIV,0,0);
            else
                error(2);
        }
        
    }
    public static void factor(Ptx tx,int lev,Pdx dx) {
        //已经读入因子的第一个sym
        //<因子>->id|'('表达式')'|num|<自定义函数调用>
        if(sym==symbol.ident){//id
            int i=position(id,tx.value);
            if(i==0)error(10);
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
        }
        else if(sym==symbol.number) {
            gen(fct.LIT,0,num);
            getsym();
        }
        else if(sym==symbol.lparen) {
            getsym();
            expression(tx,lev,dx);
            if(sym==symbol.rparen)
                getsym();
            else
                error(6);        
        }
        else
            error(15);
    }
    public static void call_intfunction(int i) {
        getsym();
        if(sym==symbol.lparen)getsym();
        else error(11);
        if(sym==symbol.rparen)getsym();
        else error(6);
//        if(sym==symbol.semicolon)getsym();
//        else error(13);
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
        System.out.println("TEST:");
        outTable();
        outInstruct();
        System.out.println("名字表尾指针tx:"+tx.value+" (tx.value包含，最后一条名字记录所处位置)");
        System.out.println("虚拟代码尾指针cx:"+cx+"(下一条代码应该处在的下一位置）");
        System.out.println("此时层中单元的相对位置dx:"+dx.value+" (dx.value下一单元应该处在的下一位置)");
        System.out.println("此时层差lev:"+lev);
    }
    //写入符号表
    public static void enter(object k,Ptx ptx,int lev,Pdx pdx) {
        ptx.value++;
        table_num++;
        table[ptx.value].name=id;
        table[ptx.value].kind=k;
        switch(k){
            case integer:    //对于整数，记录名字、类型、层以及相对位置，相对位置为该层的相对位置
                table[ptx.value].level=lev;
                table[ptx.value].adr=pdx.value;
                pdx.value++;
                break;
            case int_function:    //对于函数，记录名字、类型和层
                table[ptx.value].level=lev;
                //new add
                table[ptx.value].adr=cx;
                break;
            case void_function:
                table[ptx.value].level=lev;
                table[ptx.value].adr=cx;
                break;
        }
    }
    public static void outTable() {
        System.out.println("---------------名字表---------------------");
        System.out.println("名字name\t类型kind\t值val\t层次level\t地址adr\t大小size\t");
//        String name;            //名字
//        object kind;            //类型
//        int val;                //值
//        int level;                //层次
//        int adr;                //地址
//        int size;                //大小
        for(int i=0;i<table_num;i++) {
            System.out.println(table[i].name+"\t"+table[i].kind+"\t"+table[i].val+"\t"+table[i].level+"\t"+table[i].adr+"\t"+table[i].size+"\t");
        }
    }
    public static int position(String idt,int tx) {
        int i=tx;
        table[0].name=idt;
        while(table[i].name.equals(idt)==false)i--;
//        outTable();
        //千万注意传进来的是一个最大值table_num的值减一，不然会报空指针错
        return i;
    }
    public static void outInstruct() {
        System.out.println("---------------栈------------------------");
        for(int i=0;i<cx;i++) {
            System.out.println(i+" "+code[i].f+" "+code[i].l+" "+code[i].a);
        }
    }
    public static int gen(fct x,int y,int z) {
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
        //实例化虚拟机代码指令数组
        for(int i=0;i<cxmax;i++) code[i]=new instruct();
        //实例化名字表数组
        for(int i=0;i<txmax;i++) table[i]=new tablestruct();
//        for(int i=0;i<wsym.length;i++)
//            System.out.println(word.get(i)+" "+wsym[i]);
    }
    //单字符的符号表
    public static symbol ssym(char ch2) {
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
    public static void initC0line(String C0File) {
        //将C0文件全部读入line中
        try {
            Reader read=new FileReader(new File(C0File));
            int temp;
        //接受每一个内容
            while((temp=read.read())!=-1) {
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
            if(line[i]=='\n') {
                System.out.print("N");continue;
            }
            if(line[i]=='\r') {
                System.out.print("R");continue;
            }
            System.out.print(line[i]);
        }
            System.out.println(len);
    }
    /*
     *  漏掉空格，读取一个字符
     *  
     *  被函数getsym调用
     */
    public static int getch() {
        ch=line[cc];
        cc++;
        return 0;
    }
    /*
     * 词法分析，获取一个符号
     */
    public static int getsym() {
        while(ch=='\n'||ch=='\r'||ch=='\t'||ch==' ')
            getch();
        if(ch>='a'&&ch<='z'||ch>='A'&&ch<='Z'||ch=='_'){    //检索名字或保留字
            StringBuffer k=new StringBuffer();
            do {
                k.append(ch);
                getch();
            }while(ch>='a'&&ch<='z'||ch>='A'&&ch<='Z'||ch=='_'||(ch >= '0'&&ch <= '9'));
            id=k.toString();
            //搜索是否为保留字
            if(word.contains(id)) sym=wsym[word.indexOf(id)];
            else sym=symbol.ident;
            System.out.println(sym+": "+id);
        }
        else{                                            //不是，则检索数字
            if(ch>='0'&&ch<='9') {
                StringBuffer k=new StringBuffer();
                num=0;
                sym=symbol.number;
                do {
                    k.append(ch);
                    getch();
                }while(ch>='0'&&ch<='9');
                num=Integer.parseInt(k.toString());
                System.out.println(sym+" :"+num);
            }
            else{                                        //不是，则按照单字符处理
                sym=ssym(ch);
                if(sym!=null)
                    System.out.println(sym);
                else
                    error(2);
                getch();
            }
        }
        return 0;
    }

    public static void error(int i) {
        switch(i) {
        case 1:  System.out.println("Error 1:C0File cannot open!"); break;
        case 2:  System.out.println("Error 2:ident cannot be distinguished");break;
        case 3:  System.out.println("Error 3:Program too long,cannot generate instruct!table overflow");break;
        case 4:  System.out.println("Error 4:after ident variable's comma should  be ident variable.");break;
        case 5:  System.out.println("Error 5:after ident variables  should  be semicolon.");break;
        case 6:  System.out.println("Error 6:lose ')'");break;
        case 7:  System.out.println("Error 7:lose '{'.");break;
        case 8:  System.out.println("Error 8:should be ',' or ';'");break;
        case 9:  System.out.println("Error 9:should be ident");break;
        case 10: System.out.println("Error 10:Not Found ident.");break;
        case 11: System.out.println("Error 11:lose '('");break;
        case 12: System.out.println("Error 12:lose '}'.");break;
        case 13: System.out.println("Error 13:lose ';'.");break;
        case 14: System.out.println("Error 14:assign statement lose '='.");break;
        case 15: System.out.println("Error 15:factor error.");break;
        case 16: System.out.println("Error 16:int_function didn't define return type.");break;
        }
    }
}
