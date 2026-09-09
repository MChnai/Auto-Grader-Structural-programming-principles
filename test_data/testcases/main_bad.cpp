#include <iostream>
#include <cstdlib>        // Vi phạm: Thư viện cấm
#include <bits/stdc++.h>  // Vi phạm: Thư viện cấm

using namespace std;

int main() {
    int a, b;
    if (cin >> a >> b) {
        cout << (a + b) << endl;
    }
    
    // Vi phạm an ninh: Gọi hàm hệ thống
    system("pause");
    
    return 0;
}