#include <iostream>

// --------------------------------------------------------------------------
// TRƯỜNG HỢP 1: Cố tình chèn khoảng trắng và xuống dòng để giấu hàm system()
// --------------------------------------------------------------------------
void testCase1_WhitespaceBypass() {
    system   
        (
            "echo Test 1"
        );
}

// --------------------------------------------------------------------------
// TRƯỜNG HỢP 2: Cố tình chèn khoảng trắng trong chỉ thị thư viện #include
// --------------------------------------------------------------------------
#   include   <cstdlib>

// --------------------------------------------------------------------------
// TRƯỜNG HỢP 3: Bẫy nhiễu (Ghi từ khóa 'system()' bên trong Chuỗi in ra)
// KỲ VỌNG: Analyzer KHÔNG ĐƯỢC báo lỗi nhầm ở hàm này!
// --------------------------------------------------------------------------
void testCase3_FalsePositiveCheck() {
    std::cout << "He thong system() hoat dong rat tot va an toan!" << std::endl;
}

// --------------------------------------------------------------------------
// TRƯỜNG HỢP 4: Bẫy nhiễu (Ghi từ khóa trong Comment)
// KỲ VỌNG: Analyzer KHÔNG ĐƯỢC báo lỗi nhầm ở Comment này!
// --------------------------------------------------------------------------
/* Do not use system("pause") here */
// #include <bits/stdc++.h>

int main() {
    testCase1_WhitespaceBypass();
    testCase3_FalsePositiveCheck();
    return 0;
}