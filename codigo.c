#include <stdio.h>
#include <stdlib.h>


struct Pessoa {
    char nome[50];
    unsigned int idade;
    float altura;
    double peso;
    signed short int codigo;
};


// Função que não retorna nada
void saudacao(struct Pessoa *p) {
    printf("Olá, %s. Você tem %d anos.\n", p->nome, p->idade);
}

// Função com ponteiro e double
double multiplica(double *a, double b) {
    return (*a) * b;
}

// função usando vários tipos
void mostrarPessoa(struct Pessoa p) {
    printf("Nome: %s\n", p.nome);
    printf("Idade: %u\n", p.idade);
    printf("Altura: %.2f\n", p.altura);
    printf("Peso: %.2lf\n", p.peso);
    printf("Código: %d\n", p.codigo);
}

/* Função que soma dois inteiros
int soma(int a, int b) {
    return a + b;
}

// Declaração de variaveis globais
int x = 0;
char nome[50];*/

int main() {	
	int x;
	x = 3 + 2 * 5 + a;
	if (x) 
	{
		printf(x);
	}
	while (x) {
		x = x - 1;
	}
			
// Programa exemplo com tudo

	int x;
	float y;
	x = 10;
	y = 3.5;a += a ;
		
	if (x > 5) {
		printf(x);
	} else {
		printf(y);
	}
		
	while (x != 0) {
		x = x - 1;
	// contando para baixo
	}
	// declaração com inicialização
	int a = 5;
	float b = 3.2;
	// atribuição
	b = b + 2;
	// condicional
	if (a > 3) {
		printf("Valor de a: %d", a);
	} else{
		printf("Outro valor");
	}
		
	// repetição
	while (a > 0) {
		a -= 3;
	}
		
	while (x < 100) {
        x = x + 10;
        if (x == 50) {
            break;
        }
    }

    // Testando continue
    for (int i = 0; i < 5; i += a ) {
        if (i == 2) {
        	 continue;
		}
        printf("i = %d\n", i);
    }
		    
	int a = 5, b, c = 2;
	float x = 1.5, y;
	char letra = 'z';
	
	// expressão com variáveis
	b = a + c;
	y = x * 2.0;
	
	// operadores lógicos e relacionais
	if ((a > 3 && b < 10) || !(c == 0)) {
	    printf("Entrou no if: %d", b);	
	}
	
	// scanf (leitura)
	scanf("%d", &a);
	
	// for
	for (c = 0; c < 10; c = c + 1) {
	    printf("Contador: %d", c);
	}
	    
	const int limite = 10;
    int resultado;

    resultado = soma(3, 4);
    printf("Resultado: %d", resultado);
    int x = 5;
	do {
    	printf("%d\n", x);
    	x = x - 1;
	} while (x > 0);

	int opcao = 2;
	switch (opcao) {
    	case 1:
        	printf("Opção 1");
        	break;
    	case 2:
        	printf("Opção 2");
        	break;
    	default:
        	printf("Outra opção");
	}
	char nome[50];
	
    // array
    int v[5];
    v[0] = 1;
    v[1] = 2;

	int v[0];
    // operador ternário
    int x = (resultado > 5) ? 100 : 0;

    /* Comentário
       em várias linhas
    */
		
	return 0;
}

/*		
int soma(int a, int b) {
    return a + b;
}
*/	


