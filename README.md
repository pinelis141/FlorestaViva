# Floresta Viva + Dark Fantasy Remaster (Android)

A mesma instalação inclui **Floresta Viva** e **Cavaleiro Amaldiçoado**. O identificador do app continua `br.com.florestaviva.livewallpaper`.

## Remaster — mudanças técnicas

- A pintura original foi mantida como base, sem substituir o cavaleiro ou a catedral.
- A capa tem um atlas de **24 quadros** gerado por deformação óptica limitada da textura da própria pintura.
- O peitoral tem **16 quadros** de respiração com máscaras suaves.
- O Android **interpola pares de quadros** na taxa de renderização, evitando mudanças bruscas entre poses.
- Espada com energia programática seguindo o eixo da lâmina; olhos, velas, cinzas, névoa, reflexos, interação e evento Despertar.
- O serviço pausa sensores e renderização quando deixa de estar visível; controle de 60 FPS e modo econômico disponíveis.
- Python (`tools/make_layers.py`) cria os recursos a partir da arte-base; `tools/render_preview.py` produz a prévia MP4.

## Testes

Prévia visual em `preview/DarkFantasyRemaster-preview.mp4` — feita em Python, **não** é captura de tela do aplicativo.

Requisitos para compilar: Java 17, Android SDK 35, Gradle 8.10.2. Execute `cd android && gradle assembleDebug`.

A versão ainda requer avaliação de fluidez, possíveis artefatos em máscaras e autonomia de bateria no celular real. Os movimentos de capa não constituem uma simulação física de tecido: a textura é deformada em regiões selecionadas, preservando a silhueta pintada.
