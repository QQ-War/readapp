import SwiftUI

struct SourceListView: View {
    @StateObject private var viewModel = SourceListViewModel()

    var body: some View {
        ZStack {
            if viewModel.isLoading {
                ProgressView("正在加载书源...")
            } else if let errorMessage = viewModel.errorMessage {
                VStack {
                    Text(errorMessage)
                        .foregroundColor(.red)
                        .padding()
                    Button("重试") {
                        viewModel.fetchSources()
                    }
                }
            } else {
                List {
                    ForEach(viewModel.sources) { source in
                        NavigationLink(destination: SourceEditView(sourceId: source.bookSourceUrl)) {
                            HStack {
                                VStack(alignment: .leading, spacing: 8) {
                                    Text(source.bookSourceName)
                                        .font(.headline)
                                        .foregroundColor(source.enabled ? .primary : .secondary)
                                    
                                    if let group = source.bookSourceGroup, !group.isEmpty {
                                        Text("分组: \(group)")
                                            .font(.subheadline)
                                            .foregroundColor(.secondary)
                                    }
                                    
                                    Text(source.bookSourceUrl)
                                        .font(.caption)
                                        .foregroundColor(.gray)
                                }
                                
                                Spacer()
                                
                                Toggle("", isOn: Binding(
                                    get: { source.enabled },
                                    set: { _ in viewModel.toggleSource(source: source) }
                                ))
                                .labelsHidden()
                            }
                            .padding(.vertical, 4)
                        }
                    }
                    .onDelete(perform: deleteSource)
                }
                .refreshable {
                    viewModel.fetchSources()
                }
            }
        }
        .navigationTitle("书源管理")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                NavigationLink(destination: SourceEditView()) {
                    Image(systemName: "plus")
                }
            }
        }
        .onAppear {
            viewModel.fetchSources()
        }
    }
    
    private func deleteSource(at offsets: IndexSet) {
        offsets.forEach { index in
            let source = viewModel.sources[index]
            viewModel.deleteSource(source: source)
        }
    }
}

struct SourceListView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            SourceListView()
        }
    }
}
