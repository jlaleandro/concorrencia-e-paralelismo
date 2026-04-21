# Comparativo de Performance: Python vs Go (Web Scraping / Fetching)

## O Experimento
Realizamos um benchmark de desempenho entre dois scripts desenvolvidos para o download de 237 páginas da Wikipedia. Ambos utilizaram processamento concorrente limitado a 32 workers (threads/goroutines) para garantir uma comparação equitativa.

Ambos rodaram na mesma máquina (Mac com processador Apple Silicon, arquitetura arm64) e com acesso à mesma rede.

### Resultados Obtidos:

**Resultado em Python:**
```
Núcleos disponíveis: 8
Total pages: 237
Quantidade de theds: 32
Downloaded 237 links in 1.7422409057617188 seconds
```

**Resultado em Go:**
```
Núcleos disponíveis: 8
Total pages: 237
Quantidade de theds: 32
Downloaded 237 links in 0.474941875 seconds
```

**Análise**: O Go foi **aproximadamente 3.6x (360%) mais rápido** que o código equivalente escrito em Python.

---

## Por que o Go é tão mais rápido nessa tarefa?

Embora downloads de rede sejam o que chamamos de *I/O bound* (o gargalo costumeiramente é a velocidade da sua internet e a latência da rede, não o processador), a linguagem usada fez uma grande diferença neste caso. Eis os principais motivos:

### 1. Modelo de Concorrência (Threads em Python vs Goroutines em Go)
* **Python (`ThreadPoolExecutor`)**: Ao criar 32 threads, o Python solicita ao Sistema Operacional a criação de 32 *OS Threads* de verdade. Threads do sistema operacional sobem uma carga (stack) grande de memória (geralmente de 1MB a 8MB) e a comutação de contexto (tirar uma thread e colocar outra na CPU) é pesada para o processador (envolve interrupções profundas e syscalls do Kernel do Mac).
* **Go (`Goroutines`)**: Go utiliza algo chamado rotinas leves gerenciadas em "User Space" (conhecidas também como Green Threads). Uma goroutine inicia com pouquíssima alocação de memória (cerca de 2KB) de forma hiper-efetiva. O agendador interno (Scheduler) do Go distribui inteligentemente essas goroutines dentro das poucas Threads reais disponíveis do processador sem precisar envolver o Kernel do Mac nas trocas de contexto. 

### 2. O Global Interpreter Lock (GIL) do Python
O CPython (a implementação padrão da linguagem) usa um bloqueio de estado global para que apenas *um* ciclo de compilação da linguagem se execute por vez para não ter corrupção de variáveis em memória na máquina virtual. Embora em requisições de rede (I/O) a biblioteca `requests` consiga "pausar" a GIL, todo o restante do código que envolve pegar uma string, montar o objeto, fatiar usando `.split("/[-1]")` obriga a thread a refazer a aquisição dessa trava (GIL). No Go essa trava não existe num formato global limitante, a linguagem já nasceu para fluir o paralelismo natural.

### 3. Código Interpretado vs Código Compilado
* O **Python** é interpretado (ou convertido em *bytecode* em tempo real), o que por sua natureza gera um pequeno atraso adicional em invocações de sistema (`os.makedirs`, aberturas de sistema de arquivos) enquanto interpreta linhas da biblioteca padrão e das libs como a `urllib3` e `requests` usadas embaixo dos panos.
* O **Go** é compilado estaticamente para um binário otimizado diretamente em código de máquina (para a arquitetura Apple `darwin/arm64`).

### 4. Gestão do Cliente HTTP (`requests` vs `net/http`)
A lib nativa `net/http` do Go é vastamente elogiada no ecossistema e usada por trás de grandes orquestradoros (como Docker e Kubernetes). Ela reaproveita conexões TCP abertas com uma excelência nativa e se conecta profundamente com kqueue/epoll do sistema sem *overhead*. O requests via Python também faz bom uso do "keep-alive" de sessões, porém por padrão um gatilho de requests isolado exige recriar o pool de conexões sob a máquina virtual, o que afeta esses poucos milissegundos críticos nessa amostragem de dados.
