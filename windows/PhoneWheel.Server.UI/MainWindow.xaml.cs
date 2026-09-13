using System.Collections.Specialized;
using System.Windows;
using PhoneWheel.Server.UI.ViewModels;

namespace PhoneWheel.Server.UI;

/// <summary>
/// Janela principal da interface do servidor.
/// </summary>
public partial class MainWindow : Window
{
    private readonly MainViewModel _viewModel;

    public MainWindow()
    {
        InitializeComponent();

        _viewModel = new MainViewModel();
        DataContext = _viewModel;

        _viewModel.LogLines.CollectionChanged += OnLogLinesChanged;
        Closed += OnClosed;
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