import time
import os
import glob
import requests
from bs4 import BeautifulSoup
from urllib.parse import urljoin
from concurrent.futures import ThreadPoolExecutor
 
def get_links():
    countries_list = 'https://en.wikipedia.org/wiki/List_of_countries_by_population_(United_Nations)'
    all_links = []
    
    headers = {'User-Agent': 'Mozilla/5.0 (compatible; MyBot/1.0)'}
    
    response = requests.get(countries_list, headers=headers)
    soup = BeautifulSoup(response.text, "html.parser")
    
    countries_el = soup.select('td .flagicon+ a')
    
    for link_el in countries_el:
        link = link_el.get("href")
        link = urljoin(countries_list, link)
        all_links.append(link)
    return all_links
   
def fetch(link):
    # Cria subpasta se não existir
    os.makedirs("paises", exist_ok=True)
    response = requests.get(link)
    caminho_arquivo = os.path.join("paises", link.split("/")[-1]+".html")
    with open(caminho_arquivo, "wb") as f:
        f.write(response.content)


def limpar_htmls():
    """Remove todos os arquivos HTML da subpasta paises"""
    if os.path.exists("paises"):
        arquivos_html = glob.glob("paises/*.html")
        for arquivo in arquivos_html:
            os.remove(arquivo)
            print(f"Deletado: {arquivo}")
        if arquivos_html:
            print(f"Total removido: {len(arquivos_html)} arquivos\n")
        
 
if __name__ == '__main__':
    limpar_htmls()
    print(f"Núcleos disponíveis: {os.cpu_count()}")
    links = get_links()
    print(f"Total pages: {len(links)}")
    start_time = time.time()
    
    # Otimizado com ThreadPoolExecutor para paralelizar requisições HTTP
    # workers = número de threads que executam em paralelo
    qtd_treds = 32
    print(f"Quantidade de theds : {qtd_treds}")
    
    with ThreadPoolExecutor(max_workers=qtd_treds) as executor:
        list(executor.map(fetch, links))
 
    duration = time.time() - start_time
    print(f"Downloaded {len(links)} links in {duration} seconds")