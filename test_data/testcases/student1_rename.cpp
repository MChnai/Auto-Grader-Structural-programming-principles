// Bai lam cua Sinh Vien 1
#include <iostream>
using namespace std;
int main() {
    int number; cin >> number; // Nhap n
    long long total = 0;
    for (int index = 1; index <= number; index++) {
        total += index;
    }
    cout << total << "\n";
    return 0;
}