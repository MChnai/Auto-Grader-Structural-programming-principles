// Chuong trinh tinh tong tu 1 den N va tim gia tri lon nhat trong mang
#include <iostream>
#include <vector>
#include <algorithm>

using namespace std;

// Ham tinh tong cac so tu 1 den n
long long calculateSumOfRange(int limitNumber) {
    if (limitNumber <= 0) {
        return 0;
    }
    // Su dung cong thuc mathematically toi uu O(1)
    return (long long)limitNumber * (limitNumber + 1) / 2;
}

// Ham tim gia tri lon nhat trong danh sach
int findMaximumElement(const vector<int>& numberList) {
    if (numberList.empty()) {
        return -1;
    }
    int maximumValue = numberList[0];
    for (size_t index = 1; index < numberList.size(); ++index) {
        if (numberList[index] > maximumValue) {
            maximumValue = numberList[index];
        }
    }
    return maximumValue;
}

int main() {
    int targetNumber = 10;
    cout << "Tong: " << calculateSumOfRange(targetNumber) << endl;

    vector<int> sampleData = {3, 7, 2, 9, 5};
    cout << "Max: " << findMaximumElement(sampleData) << endl;

    return 0;
}