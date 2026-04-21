package main

import (
	"fmt"
	"io"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"sync"
	"time"

	"github.com/PuerkitoBio/goquery"
)

func getLinks() ([]string, error) {
	baseURLStr := "https://en.wikipedia.org/wiki/List_of_countries_by_population_(United_Nations)"
	baseURL, err := url.Parse(baseURLStr)
	if err != nil {
		return nil, err
	}

	req, err := http.NewRequest("GET", baseURLStr, nil)
	if err != nil {
		return nil, err
	}
	req.Header.Set("User-Agent", "Mozilla/5.0 (compatible; MyBot/1.0)")

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	doc, err := goquery.NewDocumentFromReader(resp.Body)
	if err != nil {
		return nil, err
	}

	var allLinks []string
	doc.Find("td .flagicon+ a").Each(func(i int, s *goquery.Selection) {
		link, exists := s.Attr("href")
		if exists {
			parsedLink, err := url.Parse(link)
			if err == nil {
				resolvedUrl := baseURL.ResolveReference(parsedLink)
				allLinks = append(allLinks, resolvedUrl.String())
			}
		}
	})
	return allLinks, nil
}

func fetch(link string) {
	err := os.MkdirAll("paises", os.ModePerm)
	if err != nil {
		return
	}

	resp, err := http.Get(link)
	if err != nil {
		return
	}
	defer resp.Body.Close()

	// Extraí a última parte do URL para formar o nome do arquivo
	parts := strings.Split(link, "/")
	filename := parts[len(parts)-1] + ".html"
	caminhoArquivo := filepath.Join("paises", filename)

	f, err := os.Create(caminhoArquivo)
	if err != nil {
		return
	}
	defer f.Close()

	io.Copy(f, resp.Body)
}

func limparHTMLs() {
	if _, err := os.Stat("paises"); !os.IsNotExist(err) {
		arquivos, err := filepath.Glob("paises/*.html")
		if err == nil {
			for _, arquivo := range arquivos {
				os.Remove(arquivo)
				fmt.Printf("Deletado: %s\n", arquivo)
			}
			if len(arquivos) > 0 {
				fmt.Printf("Total removido: %d arquivos\n\n", len(arquivos))
			}
		}
	}
}

func main() {
	limparHTMLs()
	fmt.Printf("Núcleos disponíveis: %d\n", runtime.NumCPU())
	
	links, err := getLinks()
	if err != nil {
		fmt.Printf("Erro ao obter links: %v\n", err)
		return
	}
	fmt.Printf("Total pages: %d\n", len(links))
	
	startTime := time.Now()

	// Quantidade de "threads"/goroutines rodando paralelamente
	qtdTreds := 32
	fmt.Printf("Quantidade de theds : %d\n", qtdTreds)

	// O channel 'sem' atua como um semáforo para limitar a concorrência
	sem := make(chan struct{}, qtdTreds)
	var wg sync.WaitGroup

	for _, link := range links {
		wg.Add(1)
		// Isso bloqueia se já houver qtdTreds em execução (o channel fica cheio)
		sem <- struct{}{} 
		
		go func(l string) {
			defer wg.Done()
			// Ao terminar, removemos um item do semáforo liberando espaço
			defer func() { <-sem }() 
			fetch(l)
		}(link)
	}

	// Aguarda todas as goroutines finalizarem
	wg.Wait()

	duration := time.Since(startTime)
	fmt.Printf("Downloaded %d links in %v seconds\n", len(links), duration.Seconds())
}
