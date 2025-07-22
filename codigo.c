#include <stdio.h>
#include <stdlib.h>


struct Pessoa {
    char nome[50];
    int idade;
    float altura;
    float peso;
    int codigo;
};

// Fun��o que n�o retorna nada
void saudacao(struct Pessoa *p) {
	p->idade = 1;
    printf("Ol�, %s. Voc� tem %d anos.\n" ,p->nome, p->idade);
}

// Fun��o com ponteiro e double
double multiplica(double *a, double b) {
    return (*a) * b ;
}

// fun��o usando v�rios tipos
void mostrarPessoa(struct Pessoa p) {
    printf("Nome: %s\n", "teste");
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

char nomeGlobal[50];
int t1;

int main() {
	
	struct Pessoa p1;
	p1.idade = 25;              // unsigned int
    p1.altura = 1.65;           // float
    p1.peso = 58.75;            // double
    p1.codigo = -123;  
    
    mostrarPessoa(p1);
    saudacao(&p1);
    
	int t1;	
	int x = 1;
	int a = 3;
	x = 3 + 2 * 5 + a;
	if (x) 
	{
		printf("%d\n", x);
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
		printf("%d\n", x);
	} else {
		printf("%.2f\n", y);
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
		printf("Valor de a: %d\n", b);
	} else{
		printf("Outro valor\n");
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
	    printf("Entrou no if: %d\n", e);	
	}
	
	// scanf (leitura)
	scanf("%d", &d);
	
	// for
	for (f = 0; f < 10; f = f + 1) {
	    printf("Contador: %d\n", f);
	}
	    
	const int limite = 10;
    int resultado;

    resultado = soma(3, 4);
    printf("Resultado: %d\n", resultado);
    x = 5;
	do {
    	printf("%d\n", x);
    	x = x - 1;
	} while (x > 0);

	int opcao = 2;
	switch (opcao) {
    	case 1:
        	printf("Op��o 1\n");
        	break;
    	case 2:
        	printf("Op��o 2\n");
        	break;
    	default:
        	printf("Outra op��o\n");
	}
	char nome[50];
	
    // array
    int v[5];
    v[0] = 1;
    v[3] = 2;
	int w[1];
    // operador tern�rio
    x = (resultado > 5) ? 100 : 0;

    /* Coment�rio
       em v�rias linhas
    */
		
	return 0;
}

