using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Threading.Tasks;
using System.Windows.Forms;
using Newtonsoft.Json;

namespace laptrinhmang
{
    public partial class Form1 : Form
    {
        public Form1()
        {
            InitializeComponent();
        }

        private async void btnLayCauThu_Click(object sender, EventArgs e)
        {
            string apiUrl = "http://localhost:5272/premierleague/cauthu";

            using (HttpClient client = new HttpClient())
            {
                try
                {
                    HttpResponseMessage response = await client.GetAsync(apiUrl);
                    response.EnsureSuccessStatusCode();

                    string json = await response.Content.ReadAsStringAsync();
                    var danhSachCauThu = JsonConvert.DeserializeObject<List<CauThu>>(json);

                    dataGridView1.DataSource = danhSachCauThu;
                }
                catch (Exception ex)
                {
                    MessageBox.Show("Lỗi khi gọi API: " + ex.Message);
                }
            }
        }

        private void dataGridView1_CellClick(object sender, DataGridViewCellEventArgs e)
        {
            if (e.RowIndex >= 0)
            {
                DataGridViewRow row = dataGridView1.Rows[e.RowIndex];

                txtMaCauThu.Text = row.Cells[0].Value?.ToString() ?? "";
                txtTenCauThu.Text = row.Cells[1].Value?.ToString() ?? "";
                txtViTri.Text = row.Cells[2].Value?.ToString() ?? "";
                txtSoAo.Text = row.Cells[3].Value?.ToString() ?? "";
                txtQuocTich.Text = row.Cells[4].Value?.ToString() ?? "";
                txtTenDoiBong.Text = row.Cells[5].Value?.ToString() ?? "";
            }
        }

        private async void btnUpdate_Click(object sender, EventArgs e)
        {
            string maCauThu = txtMaCauThu.Text;
            string apiUrl = $"http://localhost:5272/premierleague/cauthu/{maCauThu}";

            var cauThuRequest = new
            {
                maCauThu = maCauThu,
                tenCauThu = txtTenCauThu.Text,
                viTri = txtViTri.Text,
                soAo = int.TryParse(txtSoAo.Text, out int soAo) ? soAo : (int?)null,
                quocTich = txtQuocTich.Text,
                tenDoiBong = txtTenDoiBong.Text
            };

            using (HttpClient client = new HttpClient())
            {
                try
                {
                    var json = JsonConvert.SerializeObject(cauThuRequest);
                    var content = new StringContent(json, System.Text.Encoding.UTF8, "application/json");

                    HttpResponseMessage response = await client.PutAsync(apiUrl, content);
                    response.EnsureSuccessStatusCode();

                    MessageBox.Show("Cập nhật thành công!");
                    btnLayCauThu_Click(null, null);
                }
                catch (Exception ex)
                {
                    MessageBox.Show("Lỗi khi cập nhật: " + ex.Message);
                }
            }
        }

        private async void btnUpdate1_Click(object sender, EventArgs e)
        {
            string maCauThu = txtMaCauThu.Text;

            if (string.IsNullOrWhiteSpace(maCauThu))
            {
                MessageBox.Show("Vui lòng chọn cầu thủ để cập nhật!");
                return;
            }

            string apiUrl = $"http://localhost:5272/premierleague/cauthu/{maCauThu}";

            var cauThuRequest = new
            {
                maCauThu = maCauThu,
                tenCauThu = txtTenCauThu.Text,
                viTri = txtViTri.Text,
                soAo = int.TryParse(txtSoAo.Text, out int soAo) ? soAo : (int?)null,
                quocTich = txtQuocTich.Text,
                tenDoiBong = txtTenDoiBong.Text
            };

            using (HttpClient client = new HttpClient())
            {
                try
                {
                    var json = JsonConvert.SerializeObject(cauThuRequest);
                    MessageBox.Show($"URL: {apiUrl}\n\nJSON:\n{json}");

                    var content = new StringContent(json, System.Text.Encoding.UTF8, "application/json");
                    HttpResponseMessage response = await client.PutAsync(apiUrl, content);

                    MessageBox.Show($"Status Code: {response.StatusCode}");

                    string responseContent = await response.Content.ReadAsStringAsync();
                    MessageBox.Show($"Response: {responseContent}");

                    if (response.IsSuccessStatusCode)
                    {
                        MessageBox.Show("Cập nhật thành công!");
                        btnLayCauThu_Click(null, null);
                    }
                    else
                    {
                        MessageBox.Show($"Lỗi {response.StatusCode}: {responseContent}");
                    }
                }
                catch (Exception ex)
                {
                    MessageBox.Show($"Lỗi: {ex.GetType().Name} - {ex.Message}");
                }
            }
        }

        public class CauThu
        {
            public string maCauThu { get; set; }
            public string tenCauThu { get; set; }
            public string viTri { get; set; }
            public int? soAo { get; set; }
            public string quocTich { get; set; }
            public string tenDoiBong { get; set; }

            public override string ToString()
            {
                return $"{tenCauThu} - {viTri} - Số áo: {soAo}";
            }
        }

        private void button4_Click(object sender, EventArgs e)
        {
            this.Close();
        }

        private void Form1_Load(object sender, EventArgs e)
        {

        }
    }
}