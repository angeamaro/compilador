#include <stdio.h>
#include <stdlib.h>


struct Pessoa {
    char nome[50];
    unsigned int idade;
    float altura;
    double peso;
    signed short int codigo;
};


// Fun��o que n�o retorna nada
void saudacao(struct Pessoa *p) {
    printf("Ol�, %s. Voc� tem %d anos.\n", p->nome, p->idade);
}

// Fun��o com ponteiro e double
double multiplica(double *a, double b) {
    return (*a) * b ;
}

// fun��o usando v�rios tipos
void mostrarPessoa(struct Pessoa p) {
    printf("Nome: %s\n", p.nome);
    printf("Idade: %u\n", p.idade);
    printf("Altura: %.2f\n", p.altura);
    printf("Peso: %.2lf\n", p.peso);
    printf("C�digo: %d\n", p.codigo);
}

/* Fun��o que soma dois inteiros */
int soma(int a, int b) {
    return a + b;
}

// Declara��o de variaveis globais
int x = 0;
char nomeGlobal[50];

int main() {	
	int x;
	int a = 2;
	x = 3 + 2 * 5 + a;
	if (x) 
	{
		printf("%d", x);
	}
	while (x) {
		x = x - 1;
	}
			
// Programa exemplo com tudo

	int y_int;
	float y;
	x = 10;
	y = 3.5;
	a += a;
		
	if (x > 5) {
		printf("%d", x);
	} else {
		printf("%.2f", y);
	}
		
	while (x != 0) {
		x = x - 1;
		// contando para baixo
	}
	// declara��o com inicializa��o
	int b = 5;
	float c = 3.2;
	// atribui��o
	c = c + 2;
	// condicional
	if (b > 3) {
		printf("Valor de a: %d", b);
	} else{
		printf("Outro valor");
	}
		
	// repeti��o
	while (b > 0) {
		b -= 3;
	}
		
	while (x < 100) {
        x = x + 10;
        if (x == 50) {
            break;
        }
    }

	int i;
    // Testando continue
    for (i = 0; i < 5; i += a ) {
        if (i == 2) {
        	 continue;
		}
        printf("i = %d\n", i);
    }
		    
	int d = 5, e, f = 2;
	float u = 1.5, v1;
	char letra = 'z';
	
	// express�o com vari�veis
	e = d + f;
	v1 = u * 2.0;
	
	// operadores l�gicos e relacionais
	if ((d > 3 && e < 10) || !(f == 0)) {
	    printf("Entrou no if: %d", e);	
	}
	
	// scanf (leitura)
	scanf("%d", &d);
	
	// for
	for (f = 0; f < 10; f = f + 1) {
	    printf("Contador: %d", f);
	}
	    
	const int limite = 10;
    int resultado;

    resultado = soma(3, 4);
    printf("Resultado: %d", resultado);
    x = 5;
	do {
    	printf("%d\n", x);
    	x = x - 1;
	} while (x > 0);

	int opcao = 2;
	switch (opcao) {
    	case 1:
        	printf("Op��o 1");
        	break;
    	case 2:
        	printf("Op��o 2");
        	break;
    	default:
        	printf("Outra op��o");
	}
	char nome[50];
	
    // array
    int v[5];
    v[0] = 1;
    v[1] = 2;

	int w[1];
    // operador tern�rio
    x = (resultado > 5) ? 100 : 0;

    /* Coment�rio
       em v�rias linhas
    */
		
	return 0;
}

