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
    enum fct f;
    int l;
    int a;
}code[codemaxsize];

int base(int, int*, int);
void interpret();

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
            //cout << "("<<i<<") "<<str2 << " " << str3 << " " << str4 << endl;
            code[instructionsum].f = string_to_fct(str1);
            code[instructionsum].l = stoi(str2);
            code[instructionsum].a = stoi(str3);
            instructionsum++;
            //out << line << endl; // TEST
        }
    }else {
        cout << "no such file" << endl;
    }

    for (int i = 0; i < instructionsum; i++)
        cout<<"("<<i<<") "<<code[i].f<<" "<<code[i].l<<" "<<code[i].a<<endl;

    // interpret();
    system("pause");
    return 0;
}

