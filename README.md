# Floresta Viva — galeria de Live Wallpapers (Android)

Versão 0.3.0: **um único aplicativo Android** com dois cenários selecionáveis: **Floresta Viva** e **Cavaleiro Amaldiçoado (Dark Fantasy)**.

- Mantém `applicationId` `br.com.florestaviva.livewallpaper`, `ForestWallpaperService` e preferências `forest_prefs` para preservar a instalação e o tema Floresta original.
- A tela principal permite alternar de tema e aplicar o wallpaper. A mudança de preferência atualiza o serviço em execução.
- O cenário Dark Fantasy utiliza a **arte original escolhida**, além de **imagens com transparência recortadas** para capa e peitoral.
- Programação independente dos efeitos do cavaleiro: energia na espada, olhos responsivos, leve oscilação de capa e peitoral, chamas, neblina, cinzas, sensor de inclinação e Despertar por toque prolongado/duplo.
- Ambos os cenários suspendem renderização e sensor quando invisíveis, e possuem modo 15 FPS. O cavaleiro limita partículas a 12 no modo econômico.

## Limitação artística na versão 0.3

A imagem aprovada é originalmente plana. O sistema usa recortes transparentes e deslocamentos pequenos; **isso não equivale a um rig 2D completo, reconstruído atrás da capa**, e a respiração é sutil. Para animação mais convincente, os recortes devem ser refinados manualmente com reconstrução das áreas ocultas. Não há vídeo em loop.

## Gerar APK

Com JDK 17 e Android SDK 35:

```sh
cd android
gradle --no-daemon assembleDebug
```

Ou faça push no GitHub e use a workflow em `.github/workflows/android.yml`. O artefato resultante é `app-debug.apk` dentro de `FlorestaViva-APK`.

**Atenção ao atualizar uma instalação antiga:** uma compilação de teste (`debug`) do GitHub Actions pode usar uma chave diferente da versão instalada. Nesse caso o Android impedirá a atualização direta: será necessário **desinstalar a versão de teste anterior**, instalar o novo APK e reaplicar o wallpaper. Isso continua sendo **um único aplicativo**, e não dois aplicativos separados. Para permitir atualizações futuras sem reinstalar, configurar uma assinatura de lançamento persistente.
