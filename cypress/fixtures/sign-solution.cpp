//Task: sign
//Author: Galina Momcheva

#include <iostream>
using namespace std;

int main () {
   int cena1, cena2, cena3;
   int posl_cena1, nach_cena2, posl_cena2, nach_cena3, posl_cena3;

   cin >> cena1 >> cena2 >> cena3;
   posl_cena1 = cena1 % 10;
   nach_cena2 = cena2 / 10;
   posl_cena2 = cena2 % 10;
   nach_cena3 = cena3 / 10;
   posl_cena3 = cena3 % 10;
   cout <<cena1;
   if (posl_cena1 == nach_cena2)
      cout << '\'' << posl_cena2;
   else
      cout << nach_cena2 << posl_cena2;
   if (posl_cena2 == nach_cena3)
      cout << '\'' << posl_cena3;
   else
      cout << nach_cena3 << posl_cena3;
   cout << endl;
   return 0;
}
