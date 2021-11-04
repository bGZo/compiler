#include<iostream>
#include<fstream>
#include<string>
#include<sstream>
#define codemaxsize 500
#define stacksize 50
using namespace std;

enum fct {
    LIT, LOD, STO, CAL, INT, JMP, JPC, ADD, SUB, MUL, DIV, RED, WRT, RET
};
fct string_to_fct(string s){
    if (s == "LIT") return LIT;
    if (s == "LOD") return LOD;
    if (s == "STO") return STO;
    if (s == "CAL") return CAL;
    if (s == "INT") return INT;
    if (s == "JMP") return JMP;
    if (s == "JPC") return JPC;
    if (s == "ADD") return ADD;
    if (s == "SUB") return SUB;
    if (s == "MUL") return MUL;
    if (s == "DIV") return DIV;
    if (s == "RED") return RED;
    if (s == "WRT") return WRT;
    if (s == "RET") return RET;
}

struct instruction {
    enum fct f;                                 /* 操作码*/
    int l;                                      /* 层数*/
    int a;                                      /* 值*/
}code[codemaxsize];

int base(int, int*, int);
void interpret();
int s[stacksize];

int main(){
    string filename;
    string line;
    string str1, str2, str3;
    int instructionsum = 0;

    ifstream in("middleCode.txt");
    ofstream out("2.txt");
    if(in){
        while (getline(in, line)){
            cout << line << endl;
            istringstream is(line);
            is >> str1 >> str2 >> str3;
            //cout << "("<<i<<") "<<str2 << " " << str3 << " " << str4 << endl; /*TEST*/
            code[instructionsum].f = string_to_fct(str1);
            code[instructionsum].l = stoi(str2);
            code[instructionsum].a = stoi(str3);
            instructionsum++;
            //out << line << endl; /* TEST*/
        }
    }else {
        cout << "no such file" << endl;
        return 0;
    }
    
    interpret();
    system("pause");
    return 0;
}


void interpret(){
    int p;                                  /* 指令寄存器*/
    int b;                                  /* 基址寄存器*/
    int t;                                  /* 栈顶寄存器*/
    struct instruction i;                   /* 虚拟机代码段*/

    cout << "----------start----------\n";
    t = 0; b = 0; p = 0;
    s[0] = s[1] = s[2] = 0;
    do {
        i = code[p];
        p++;
        switch (i.f){
            case LIT:
                s[t] = i.a;
                t++;
                break;
            case LOD:
                if(i.a==0&&i.l==0){
                    s[t]=s[0];
                    t++;
                }else {
                    s[t] = s[base(i.l, s, b) + i.a];
                    t++;
                }
                break;
            case STO:
                t--;
                if(i.a==0&&i.l==0){
                    s[0]=s[t];
                }else {
                    s[base(i.l, s, b) + i.a] = s[t];
                }
                break;
            case CAL:                       /* 存放当前信息, 更新指针地址*/
                s[t] = base(i.l, s, b);     /* 基址入栈*/
                s[t + 1] = 0;               /* 初始化本过程的基址*/
                s[t + 2] = p;               /* 当前指令指针入栈*/
                b = t;                      /* 改变基地址指针为新过程的基地址*/
                p = i.a;                    /* 跳转*/
                break;
            case INT:
                t += i.a;
                break;
            case JMP:
                p = i.a;
                break;
            case JPC:
                t--;
                if (s[t] == 0)
                    p = i.a;
                break;
            case ADD:
                t--;
                s[t - 1] = s[t] + s[t - 1];
                break;
            case SUB:
                t--;
                s[t - 1] = s[t - 1] - s[t];
                break;
            case MUL:
                t--;
                s[t - 1] = s[t - 1] * s[t];
                break;
            case DIV:
                t--;
                s[t - 1] = s[t - 1] / s[t];
                break;
            case RED:
                cin >> s[t];
                t++;
                break;
            case WRT:
                cout << s[t - 1]<<endl;
                t--;
                break;
            case RET:
                t = b;
                p = s[t + 2];
                b = s[t];
                if (p == 0) return ;            /* NOTE: if b==0: return 0; call will not return!!!*/
                break;
        }
    //    for(int i=0; i<stacksize; i++) cout<<s[i]<<' '; cout<<endl; /*TEST Stack*/
    } while (p != 0);
}

int base(int l, int* s, int b){ /* 求得上层过程的基址 */
    int bt;
    bt = b;
    while (l > 0){
        bt = s[bt];
        l--;
    }
    return bt;
}
