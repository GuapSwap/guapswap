package app

import tui._
import tui.crossterm.CrosstermJni
import tui.widgets.tabs.TabsWidget
import tui.widgets.BlockWidget
import tui.widgets.BlockWidget.BorderType

object GuapSwapApp {
    case class App(
                    titles: Array[String],
                    var index: Int = 0
                  ) {

        def next(): Unit =
            index = (index + 1) % titles.length

        def previous(): Unit =
            if (index > 0) {
                index -= 1
            } else {
                index = titles.length - 1
            }
    }
    def main(args: Array[String]): Unit = withTerminal { (jni, terminal) =>
        // create app and run it
        val app = App(titles = Array("GuapSwap", "Logs"))
        run_app(terminal, app, jni);
    }

    def run_app(terminal: Terminal, app: App, jni: CrosstermJni): Unit =
        while (true) {
            terminal.draw(f => ui(f, app))

            jni.read() match {
                case key: tui.crossterm.Event.Key =>
                    key.keyEvent.code match {
                        case char: tui.crossterm.KeyCode.Char if char.c() == 'q' => return
                        case _: tui.crossterm.KeyCode.Right                      => app.next()
                        case _: tui.crossterm.KeyCode.Left                       => app.previous()
                        case _                                                   => ()
                    }
                case _ => ()
            }
        }

    def guapswap_widget(bottomChunk: Rect)(implicit f: Frame): Unit = {

        val chunks = Layout(
            direction = Direction.Horizontal,
            constraints = Array(Constraint.Percentage(50), Constraint.Percentage(50)) // Add more constraints here if we want to display additional blocks
        )
          .split(bottomChunk)

        val block0 = BlockWidget(title = Some(Spans.styled("Mining Portfolios", Style(fg=Some(Color.Black)))), borders = Borders.ALL, borderType = BorderType.Plain, borderStyle = Style(fg=Some(Color.Red)))
        f.renderWidget(block0, chunks(0))

        val block1 = BlockWidget(title = Some(Spans.styled("Swaps", Style(fg=Some(Color.Black)))), borders = Borders.ALL, borderType = BorderType.Plain, borderStyle = Style(fg=Some(Color.Red)))
        f.renderWidget(block1, chunks(1))

    }

    def logs_widget(bottomChunk: Rect)(implicit f: Frame): Unit = {

        val block0 = BlockWidget(title = Some(Spans.styled("Program Logs", Style(fg = Some(Color.Black)))), borders = Borders.ALL, borderType = BorderType.Plain, borderStyle = Style(fg = Some(Color.Red)))
        f.renderWidget(block0, bottomChunk)

    }

    def ui(implicit f: Frame, app: App): Unit = {
        val chunks = Layout(
            direction = Direction.Vertical,
            margin = Margin(3, 3),
            constraints = Array(Constraint.Length(3), Constraint.Min(0))
        ).split(f.size)

        val block = BlockWidget(style = Style(bg = Some(Color.White), fg = Some(Color.White)))
        f.renderWidget(block, f.size)
        val titles = app.titles
          .map { t =>
              val (first, rest) = t.splitAt(1)
              Spans.from(
                  Span.styled(first, Style(fg = Some(Color.Black))),
                  Span.styled(rest, Style(fg = Some(Color.Black)))
              )
          }

        val tabs = TabsWidget(
            titles = titles,
            block = Some(BlockWidget(borders = Borders.ALL, borderType=BorderType.Plain, borderStyle=Style(fg=Some(Color.Red)), title = None)),
            selected = app.index,
            style = Style(fg = Some(Color.Black)),
            highlightStyle = Style(addModifier = Modifier.UNDERLINED)
        )
        f.renderWidget(tabs, chunks(0))

        val widget_fun: (Rect => Unit) = app.index match {
            case 0 => guapswap_widget
            case 1 => logs_widget
            case _ => sys.error("unreachable")
        }
        widget_fun(chunks(1))
    }
}
