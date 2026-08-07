package com.hggabriel.pokerun

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.hggabriel.pokerun.ui.theme.PokerunTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Barras de sistema fixas em claro: o app não segue o tema do aparelho (D-13).
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            PokerunTheme {
                CascaDeNavegacao()
            }
        }
    }
}

/**
 * Casca de navegação: `Scaffold` com `NavigationBar`, nunca `NavigationSuiteScaffold`
 * (docs/03 §1). O adaptativo troca a barra por rail lateral em largura expandida, e
 * isso cobraria especificação e teste de uma segunda navegação — incluindo onde ficam
 * cabeçalho e engrenagem — para atender ninguém: a premissa é celular no bolso, num
 * grupo de oito.
 *
 * **O corpo está vazio de propósito.** As rotas type-safe, a pilha por destino, o
 * comportamento do voltar e o estado selecionado pelos três canais são `F1-T07`. Esta
 * tarefa só tira o template do caminho para que nenhuma tela nasça dentro dele.
 *
 * Não se chama `PokerunApp`: `F0-T04` reserva esse nome para a `Application` que
 * hospeda o `AppContainer`, e classe e função homônimas no mesmo pacote colidem na
 * chamada.
 */
@Composable
fun CascaDeNavegacao() {
    var destinoAtual by rememberSaveable { mutableStateOf(DestinoDeTopo.HOJE) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                DestinoDeTopo.entries.forEach { destino ->
                    val selecionado = destino == destinoAtual
                    NavigationBarItem(
                        selected = selecionado,
                        onClick = { destinoAtual = destino },
                        icon = {
                            Icon(
                                imageVector = if (selecionado) {
                                    destino.iconePreenchido
                                } else {
                                    destino.iconeDeContorno
                                },
                                // O rótulo já nomeia o item para o TalkBack. Repetir
                                // aqui faria o leitor de tela dizer o nome duas vezes.
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(destino.rotulo)) },
                    )
                }
            }
        },
    ) { espacamento ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(espacamento)
        )
    }
}

/**
 * Os três destinos das Fases 1 a 3 (docs/03 §2). `Pokédex` é o quarto e só entra na
 * Fase 4: mostrar uma aba vazia e bloqueada durante dois meses não é aceitável.
 *
 * Contorno para não selecionado, preenchido para selecionado, do mesmo conjunto
 * (docs/03 §1). **Os glifos são provisórios.** `material-icons-core` não tem gráfico
 * nem grupo, e `material-icons-extended` seria dependência nova, fora da lista da
 * Fase 0. A escolha definitiva é de `F1-T07`, junto dos outros dois canais do estado
 * selecionado — rótulo em `tinta-fraca` para `leitura` e filete de 2dp no topo.
 */
enum class DestinoDeTopo(
    @param:StringRes val rotulo: Int,
    val iconeDeContorno: ImageVector,
    val iconePreenchido: ImageVector,
) {
    HOJE(R.string.destino_hoje, Icons.Outlined.Home, Icons.Filled.Home),
    PROGRESSO(R.string.destino_progresso, Icons.Outlined.DateRange, Icons.Filled.DateRange),
    GRUPO(R.string.destino_grupo, Icons.Outlined.Person, Icons.Filled.Person),
}

@Preview(showBackground = true)
@Composable
private fun CascaDeNavegacaoPreview() {
    PokerunTheme {
        CascaDeNavegacao()
    }
}
