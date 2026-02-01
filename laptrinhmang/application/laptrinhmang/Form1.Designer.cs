namespace laptrinhmang
{
    partial class Form1
    {
        /// <summary>
        ///  Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        ///  Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        /// <summary>
        ///  Required method for Designer support - do not modify
        ///  the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            System.ComponentModel.ComponentResourceManager resources = new System.ComponentModel.ComponentResourceManager(typeof(Form1));
            button1 = new Button();
            dataGridView1 = new DataGridView();
            button2 = new Button();
            button4 = new Button();
            txtMaCauThu = new TextBox();
            txtTenCauThu = new TextBox();
            txtViTri = new TextBox();
            txtSoAo = new TextBox();
            txtQuocTich = new TextBox();
            pictureBox1 = new PictureBox();
            txtTenDoiBong = new TextBox();
            ((System.ComponentModel.ISupportInitialize)dataGridView1).BeginInit();
            ((System.ComponentModel.ISupportInitialize)pictureBox1).BeginInit();
            SuspendLayout();
            // 
            // button1
            // 
            button1.BackColor = Color.White;
            button1.Location = new Point(42, 437);
            button1.Name = "button1";
            button1.Size = new Size(94, 29);
            button1.TabIndex = 0;
            button1.Text = "load";
            button1.UseVisualStyleBackColor = false;
            button1.Click += btnLayCauThu_Click;
            // 
            // dataGridView1
            // 
            dataGridView1.AutoSizeColumnsMode = DataGridViewAutoSizeColumnsMode.Fill;
            dataGridView1.BackgroundColor = Color.White;
            dataGridView1.ColumnHeadersHeightSizeMode = DataGridViewColumnHeadersHeightSizeMode.AutoSize;
            dataGridView1.Location = new Point(436, 89);
            dataGridView1.Name = "dataGridView1";
            dataGridView1.RowHeadersWidth = 51;
            dataGridView1.Size = new Size(666, 402);
            dataGridView1.TabIndex = 1;
            dataGridView1.CellClick += dataGridView1_CellClick;
            // 
            // button2
            // 
            button2.BackColor = SystemColors.ButtonHighlight;
            button2.Location = new Point(160, 437);
            button2.Name = "button2";
            button2.Size = new Size(94, 29);
            button2.TabIndex = 2;
            button2.Text = "update";
            button2.UseVisualStyleBackColor = false;
            button2.Click += btnUpdate1_Click;
            // 
            // button4
            // 
            button4.BackColor = Color.White;
            button4.Location = new Point(278, 437);
            button4.Name = "button4";
            button4.Size = new Size(94, 29);
            button4.TabIndex = 4;
            button4.Text = "exit";
            button4.UseVisualStyleBackColor = false;
            button4.Click += button4_Click;
            // 
            // txtMaCauThu
            // 
            txtMaCauThu.Location = new Point(42, 89);
            txtMaCauThu.Name = "txtMaCauThu";
            txtMaCauThu.PlaceholderText = "Mã cầu thủ";
            txtMaCauThu.Size = new Size(352, 27);
            txtMaCauThu.TabIndex = 5;
            // 
            // txtTenCauThu
            // 
            txtTenCauThu.Location = new Point(42, 144);
            txtTenCauThu.Name = "txtTenCauThu";
            txtTenCauThu.PlaceholderText = "Tên cầu thủ";
            txtTenCauThu.Size = new Size(352, 27);
            txtTenCauThu.TabIndex = 6;
            // 
            // txtViTri
            // 
            txtViTri.Location = new Point(42, 199);
            txtViTri.Name = "txtViTri";
            txtViTri.PlaceholderText = "Vị trí";
            txtViTri.Size = new Size(352, 27);
            txtViTri.TabIndex = 7;
            // 
            // txtSoAo
            // 
            txtSoAo.Location = new Point(42, 254);
            txtSoAo.Name = "txtSoAo";
            txtSoAo.PlaceholderText = "Số áo";
            txtSoAo.Size = new Size(352, 27);
            txtSoAo.TabIndex = 8;
            // 
            // txtQuocTich
            // 
            txtQuocTich.Location = new Point(42, 309);
            txtQuocTich.Name = "txtQuocTich";
            txtQuocTich.PlaceholderText = "Quốc tịch";
            txtQuocTich.Size = new Size(352, 27);
            txtQuocTich.TabIndex = 9;
            // 
            // pictureBox1
            // 
            pictureBox1.BackColor = Color.Transparent;
            pictureBox1.Image = (Image)resources.GetObject("pictureBox1.Image");
            pictureBox1.Location = new Point(12, 4);
            pictureBox1.Name = "pictureBox1";
            pictureBox1.Size = new Size(514, 79);
            pictureBox1.TabIndex = 10;
            pictureBox1.TabStop = false;
            // 
            // txtTenDoiBong
            // 
            txtTenDoiBong.Location = new Point(42, 364);
            txtTenDoiBong.Name = "txtTenDoiBong";
            txtTenDoiBong.PlaceholderText = "Đội Bóng";
            txtTenDoiBong.Size = new Size(352, 27);
            txtTenDoiBong.TabIndex = 11;
            // 
            // Form1
            // 
            AutoScaleDimensions = new SizeF(8F, 20F);
            AutoScaleMode = AutoScaleMode.Font;
            BackgroundImage = (Image)resources.GetObject("$this.BackgroundImage");
            ClientSize = new Size(1132, 513);
            Controls.Add(txtTenDoiBong);
            Controls.Add(pictureBox1);
            Controls.Add(txtQuocTich);
            Controls.Add(txtSoAo);
            Controls.Add(txtViTri);
            Controls.Add(txtTenCauThu);
            Controls.Add(txtMaCauThu);
            Controls.Add(button4);
            Controls.Add(button2);
            Controls.Add(dataGridView1);
            Controls.Add(button1);
            Name = "Form1";
            Text = "Form1";
            Load += Form1_Load;
            ((System.ComponentModel.ISupportInitialize)dataGridView1).EndInit();
            ((System.ComponentModel.ISupportInitialize)pictureBox1).EndInit();
            ResumeLayout(false);
            PerformLayout();
        }

        #endregion

        private Button button1;
        private DataGridView dataGridView1;
        private Button button2;
        private Button button4;
        private TextBox txtMaCauThu;
        private TextBox txtTenCauThu;
        private TextBox txtViTri;
        private TextBox txtSoAo;
        private TextBox txtQuocTich;
        private PictureBox pictureBox1;
        private TextBox txtTenDoiBong;
    }
}
