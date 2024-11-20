import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;



class Bairro {
    private String nome;
    private int populacao;
    private double limiteDiario;
    private List<Double> consumoDiario;

    // Construtor da classe Bairro
    public Bairro(String nome, int populacao) {
        this.nome = nome;
        this.populacao = populacao;
        this.limiteDiario = populacao * 110; // Limite diário é a população * 110
        this.consumoDiario = new ArrayList<>();
    }

    // Método para obter a população do bairro
    public int getPopulacao() {
        return populacao;
    }

    // Método para adicionar consumo diário
    public void adicionarConsumoDiario(double consumo) {
        consumoDiario.add(consumo);
    }


    // Método para calcular o consumo total
    public double calcularConsumoTotal() {
        return consumoDiario.stream().mapToDouble(Double::doubleValue).sum();
    }

    // Método para verificar o status do consumo em relação ao limite
    public String verificarConsumo(double consumo) {
        double percentualExcedente = ((consumo - limiteDiario) / limiteDiario) * 100;
        percentualExcedente = Math.round(percentualExcedente * 100.0) / 100.0; // Arredonda para 2 casas decimais

        if (percentualExcedente <= 0) {
            return "Consumo ideal. Você está dentro do limite!";
        } else if (percentualExcedente <= 25) {
            return "Consumo alto. Tente reduzir o uso de água!";
        } else if (percentualExcedente <= 50) {
            return "Consumo muito alto. Ações de economia são necessárias!";
        } else if (percentualExcedente <= 75) {
            return "Consumo extremamente alto! Urgente redução no consumo!";
        } else {
            return "Bairro em estado de alerta! Consumo excessivo de água!";
        }
    }

    // Métodos para acessar o nome, limite diário e lista de consumos diários
    public String getNome() {
        return nome;
    }

    public double getLimiteDiario() {
        return limiteDiario;
    }

    public List<Double> getConsumoDiario() {
        return consumoDiario;
    }
}

public class ControleHidrico {
    // Método para conectar ao banco de dados MySQL
    private static Connection conectar() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/controle_hidrico"; // URL do banco de dados
        String usuario = "root"; // Usuário do MySQL
        String senha = "030506"; // Sua senha do MySQL
        return DriverManager.getConnection(url, usuario, senha);
    }

    // Método para salvar o bairro no banco de dados
    private static void salvarBairro(Bairro bairro) throws SQLException {
        String sql = "INSERT INTO bairro (nome, populacao, limite_diario) VALUES (?, ?, ?)";

        try (Connection con = conectar(); PreparedStatement stmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, bairro.getNome());
            stmt.setInt(2, bairro.getPopulacao()); // Usando o método getPopulacao()
            stmt.setDouble(3, bairro.getLimiteDiario());
            stmt.executeUpdate();

            // Obtém o ID do bairro recém-inserido
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int bairroId = rs.getInt(1); // O ID gerado para o bairro

                // Salva os consumos diários
                salvarConsumos(bairroId, bairro);
            }
        }
    }

    // Método para salvar os consumos de um bairro
    private static void salvarConsumos(int bairroId, Bairro bairro) throws SQLException {
        String sql = "INSERT INTO consumo (bairro_id, consumo_diario, data) VALUES (?, ?, CURDATE())";

        try (Connection con = conectar(); PreparedStatement stmt = con.prepareStatement(sql)) {
            for (Double consumo : bairro.getConsumoDiario()) {
                stmt.setInt(1, bairroId);
                stmt.setDouble(2, consumo);
                stmt.executeUpdate();
            }
        }
    }

    public static void main(String[] args) {
        Scanner scn = new Scanner(System.in);
        boolean continuar = true;

        System.out.println("Bem-vindo ao Sistema de Controle Hídrico!");
        System.out.println("Aqui você pode monitorar e melhorar o consumo de água nos bairros da sua cidade.");

        while (continuar) {
            System.out.print("Digite o nome do bairro: ");
            String nome = scn.nextLine();

            int populacao = 0;
            while (true) {
                System.out.print("Digite a população do bairro " + nome + " (em milhares): ");
                if (scn.hasNextInt()) {
                    populacao = scn.nextInt() * 1000;
                    if (populacao <= 0) {
                        System.out.println("A população deve ser um número positivo.");
                    } else {
                        break;
                    }
                } else {
                    System.out.println("Por favor, digite um número válido.");
                    scn.next(); // Limpa o buffer
                }
            }

            scn.nextLine(); // Limpa o buffer da próxima linha

            Bairro bairro = new Bairro(nome, populacao);

            int numDias = 0;
            while (true) {
                System.out.print("Quantos dias você deseja registrar o consumo para o bairro " + nome + "? ");
                if (scn.hasNextInt()) {
                    numDias = scn.nextInt();
                    if (numDias <= 0) {
                        System.out.println("O número de dias deve ser positivo.");
                    } else {
                        break;
                    }
                } else {
                    System.out.println("Por favor, digite um número válido.");
                    scn.next(); // Limpa o buffer
                }
            }

            scn.nextLine(); // Limpa o buffer da próxima linha

            for (int i = 0; i < numDias; i++) {
                double consumoDiario = 0;
                while (true) {
                    System.out.print("Digite o consumo de água em litros para o dia " + (i + 1) + " do bairro " + nome + ": ");
                    if (scn.hasNextDouble()) {
                        consumoDiario = scn.nextDouble();
                        if (consumoDiario <= 0) {
                            System.out.println("O consumo diário deve ser um valor positivo.");
                        } else {
                            break;
                        }
                    } else {
                        System.out.println("Por favor, digite um número válido.");
                        scn.next(); // Limpa o buffer
                    }
                }

                scn.nextLine(); // Limpa o buffer da próxima linha
                bairro.adicionarConsumoDiario(consumoDiario);

                System.out.println("Consumo do dia " + (i + 1) + ": " + consumoDiario + " L - " + bairro.verificarConsumo(consumoDiario));
            }

            // Salva os dados no banco de dados
            try {
                salvarBairro(bairro);
                System.out.println("\nDados do bairro " + bairro.getNome() + " salvos com sucesso no banco de dados!");
            } catch (SQLException e) {
                System.out.println("Erro ao salvar os dados no banco de dados: " + e.getMessage());
            }

            System.out.print("\nDeseja controlar outro bairro? (s/n): ");
            char resposta = scn.next().charAt(0);
            scn.nextLine(); // Limpa o buffer

            if (resposta != 's' && resposta != 'S') {
                continuar = false;
            }
        }

        System.out.println("\nObrigado por usar o Sistema de Controle Hídrico!");
        scn.close();
    }
}
