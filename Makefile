# Nome do arquivo principal (com pacote)
MAIN_CLASS=main.Main

# Diretórios
SRC_DIR=src
BIN_DIR=bin

# Coleta todos os .java recursivamente
SOURCES=$(shell find $(SRC_DIR) -name "*.java")

# Alvo padrão
all: compile run1

# Compila os .java para o diretório bin
compile:
	@echo "Compilando..."
	@mkdir -p $(BIN_DIR)
	@javac -d $(BIN_DIR) $(SOURCES)

# Executa a classe principal
run:
	gcc $(IN) -o $(OUT)
	java -cp $(BIN_DIR) $(MAIN_CLASS) $(IN) $(OUT)

run1:
	gcc codigo.c -o codigo.exe
	java -cp $(BIN_DIR) $(MAIN_CLASS) 


# Limpa os arquivos .class
clean:
	@echo "Limpando arquivos compilados..."
	@rm -rf $(BIN_DIR)
	@rm *.exe
# Apenas compilar (sem rodar)
build: compile

# Ajuda
help:
	@echo "Comandos disponíveis:"
	@echo "  make           -> compilar e executar"
	@echo "  make compile   -> compilar apenas"
	@echo "  make run       -> executar (após compilar)"
	@echo "  make clean     -> apagar arquivos compilados"
	@echo "  make build     -> alias para compilar"
