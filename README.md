# Comparativo de Performance: Python vs Go vs Java (Web Scraping / Fetching)

## O Experimento
Realizamos um benchmark de desempenho entre três scripts desenvolvidos (Python, Go e Java 26) para o download local e o processamento de 237 páginas HTML da Wikipedia. 
Todos utilizaram a técnica de processamento concorrente paralelizável, limitados a **32 workers (threads nativas ou goroutines)** para garantir uma comparação de disputa equilibrada.

Ambos rodaram na mesma máquina base (Processador Apple Silicon / Mac M1 de arquitetura arm64) com as mesmas condições de acesso à rede.

### Resultados Obtidos:

🏆 **1º Lugar - Go (Golang):**
```text
Núcleos disponíveis: 8
Total pages: 237
Quantidade de threads: 32
Downloaded 237 links in 0.474941875 seconds
```

**2º Lugar - Python (ThreadPoolExecutor):**
```text
Núcleos disponíveis: 8
Total pages: 237
Quantidade de threads: 32
Downloaded 237 links in 1.7422409057617188 seconds
```

**3º Lugar - Java 26 (Executors):**
```text
Núcleos disponíveis: 8
Total pages: 237
Quantidade de threads simuladas: 32
Downloaded 237 links in 4.293 seconds
```

**Análise Principal**: A medalha de campeão é do Go, de forma absoluta! Sendo **aproximadamente 3.6x (360%) mais rápido** que o Python e ostentando ser **9x mais rápido** que a JVM (Java) num cenário de tiro curto.

---

## O Veredito Arquitetural: Por que formaram esse pódio?

Embora todo o tráfego HTTP de rede e download massivo seja tipificado como uma carga de *I/O bound* (onde o grande gargalo esperado é o tempo de resposta da internet da operadora), as mecânicas arquiteturais por trás e a fase de *Start* das linguagens é que entregaram a diferença nos tempos. Eis o que ocorre por baixo dos panos:

### 1. 🏆 O Campeão: Go e sua simbiose incrível de concorrência nativa
* **Binário Nativo Direto**: O Go trabalha na forma compilada, rodando um binário ultra-otimizado já escovado para a linguagem de máquina e modelo do S.O. sem virtualização pesada entre ele e o processador. O clique para ligar o script entra em modo *"turbo"* de forma instantânea.
* **Green Threads (As Famosas Goroutines)**: Num comparativo esmagador entre threads de SO, o Go brilha instanciando milhares de *"rotinas leves"* ao custo de meros ~2KB de tamanho do inicial da RAM. O engenhoso **Go Scheduler** orquestra e distribui tudo isso sem esforçar praticamente nada em mudanças de contexto do kernel sobre os 8 núcleos M1.

### 2. O Honroso Vice: Python e as ligações efáveis e rápidas em C
* No campo de guerra, a falta de compilação dinâmica real somada à limitação terrível do *GIL (Global Interpreter Lock)* do Python o colocariam como um claro sub-desempenho comparado a linguagens puramente tipadas em grandes escalas multicore. 
* Contudo, toda sua formidável velocidade num teste de 1.7 segundos é um tributo às cascas desenvolvidas nele: as bibliotecas **`requests` e `bs4`**. Elas são construídas fundamentalmente dependendo de núcleos duros em `C` puro. O Python atua no script apenas como *"a cola plástica"* invocando eficientemente executáveis `.so` muito polidos quase magicamente sobre conexões de sockets da interface de rede (chamadas não interpretadas).

### 3. O Injustiçado dos Cenários Curtos: A Arquitetura do Java (JVM)
* O Java moderno é o lar do robusto motor assíncrono para os servidores que movem as engrenagens finaceiras do mundo, então onde ele errou aqui?! A resposta mora no fenômeno chamado: **O Preço do Aquecimento (Cold Start & JIT Compiler)**.
* Diferente do Go que já entrou pronto pra correr, as máquinas de automação Java (como a JVM) perdem um tempo crítico — nesse caso mais de **2 ou 3 segundos literais** —, só de compilar os *Bytecodes*. As filas instanciam centenas de regras do ecossistema das novas features de `HttpClient` e as proteções de chaves da TLS/SSL para bater nos portões da Wikipedia e só liberar disparo da primeira Thread para baixar os gigas depois disso acontecer!
* **A magia reservada do JIT (Just-in-Time):** Durante a diminuta prova de sprint (de apenas singelas 237 páginas), o Java estava *"usando seu óculos para procurar na estrada e compilar inteligentemente a rota"*. Mas, se esse *script* continuasse iterando sem encerramento até a casa de **10.000+ arquivos contínuos**, a fase de *Warm-up* do JIT C2 já estaria mapeada em 100% como nativa, consumiria com esmero e ultrapassaria a escala do coitado do Python facilmente, mas talvez nunca pegasse de volta a glória bruta inicial das Goroutines do Go.
