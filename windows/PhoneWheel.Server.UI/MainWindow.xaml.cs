using System.Collections.Specialized;
using System.Windows;
using Microsoft.Web.WebView2.Core;
using PhoneWheel.Server.UI.ViewModels;

namespace PhoneWheel.Server.UI;

/// <summary>
/// Janela principal da interface do servidor.
/// </summary>
public partial class MainWindow : Window
{
    private const string ControllerTesterUrl = "https://controllertest.io/pt/embed/gamepad-mapping";

    private readonly MainViewModel _viewModel;

    public MainWindow()
    {
        InitializeComponent();

        _viewModel = new MainViewModel();
        DataContext = _viewModel;

        _viewModel.LogLines.CollectionChanged += OnLogLinesChanged;
        Closed += OnClosed;
        Loaded += OnLoaded;
    }

    private async void OnLoaded(object? sender, RoutedEventArgs e)
    {
        try
        {
            await ControllerTester.EnsureCoreWebView2Async();
                        ControllerTester.Source = new Uri(ControllerTesterUrl);
        }
        catch (Exception ex)
        {
            MessageBox.Show(
                "Não foi possível carregar o testador de controle (controllertest.io).\n\n" +
                "Verifique se o WebView2 Runtime está instalado.\n" +
                $"Detalhe: {ex.Message}",
                "Teste de controle",
                MessageBoxButton.OK,
                MessageBoxImage.Warning);
        }
    }

    private void OnLogLinesChanged(object? sender, NotifyCollectionChangedEventArgs e)
    {
        if (e.Action == NotifyCollectionChangedAction.Add && LogList.Items.Count > 0)
        {
            LogList.ScrollIntoView(LogList.Items[LogList.Items.Count - 1]);
        }
    }

    private void OnClosed(object? sender, EventArgs e)
    {
        _viewModel.LogLines.CollectionChanged -= OnLogLinesChanged;
        _viewModel.Dispose();
    }
}