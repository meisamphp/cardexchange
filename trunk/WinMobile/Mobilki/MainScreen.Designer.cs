namespace Mobilki
{
    partial class MainScreen
    {
        /// <summary>
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;
        private System.Windows.Forms.MainMenu mainMenu1;

        /// <summary>
        /// Clean up any resources being used.
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
        /// Required method for Designer support - do not modify
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            this.mainMenu1 = new System.Windows.Forms.MainMenu();
            this.menuItem = new System.Windows.Forms.MenuItem();
            this.exchangeMenuItem = new System.Windows.Forms.MenuItem();
            this.showSettingsMenuItem = new System.Windows.Forms.MenuItem();
            this.exitMenuItem = new System.Windows.Forms.MenuItem();
            this.label1 = new System.Windows.Forms.Label();
            this.stateLabel = new System.Windows.Forms.Label();
            this.SuspendLayout();
            // 
            // mainMenu1
            // 
            this.mainMenu1.MenuItems.Add(this.menuItem);
            this.mainMenu1.MenuItems.Add(this.exitMenuItem);
            // 
            // menuItem
            // 
            this.menuItem.MenuItems.Add(this.exchangeMenuItem);
            this.menuItem.MenuItems.Add(this.showSettingsMenuItem);
            this.menuItem.Text = "CardExchange";
            // 
            // exchangeMenuItem
            // 
            this.exchangeMenuItem.Text = "Exchange";
            this.exchangeMenuItem.Click += new System.EventHandler(this.button_Click);
            // 
            // showSettingsMenuItem
            // 
            this.showSettingsMenuItem.Text = "Settings";
            this.showSettingsMenuItem.Click += new System.EventHandler(this.showSettingsScreen);
            // 
            // exitMenuItem
            // 
            this.exitMenuItem.Text = "Exit";
            this.exitMenuItem.Click += new System.EventHandler(this.exitMenuItemClicked);
            // 
            // label1
            // 
            this.label1.Anchor = ((System.Windows.Forms.AnchorStyles)(((System.Windows.Forms.AnchorStyles.Top | System.Windows.Forms.AnchorStyles.Left)
                        | System.Windows.Forms.AnchorStyles.Right)));
            this.label1.Font = new System.Drawing.Font("Tahoma", 12F, System.Drawing.FontStyle.Bold);
            this.label1.Location = new System.Drawing.Point(17, 10);
            this.label1.Name = "label1";
            this.label1.Size = new System.Drawing.Size(135, 27);
            this.label1.Text = "CardExchange";
            this.label1.TextAlign = System.Drawing.ContentAlignment.TopCenter;
            // 
            // stateLabel
            // 
            this.stateLabel.Anchor = ((System.Windows.Forms.AnchorStyles)((System.Windows.Forms.AnchorStyles.Left | System.Windows.Forms.AnchorStyles.Right)));
            this.stateLabel.Location = new System.Drawing.Point(35, 50);
            this.stateLabel.Name = "stateLabel";
            this.stateLabel.Size = new System.Drawing.Size(100, 20);
            this.stateLabel.TextAlign = System.Drawing.ContentAlignment.TopCenter;
            // 
            // MainScreen
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(96F, 96F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Dpi;
            this.AutoScroll = true;
            this.ClientSize = new System.Drawing.Size(176, 180);
            this.Controls.Add(this.stateLabel);
            this.Controls.Add(this.label1);
            this.Menu = this.mainMenu1;
            this.Name = "MainScreen";
            this.Text = "CardExchange";
            this.ResumeLayout(false);

        }

        #endregion

        private System.Windows.Forms.Label label1;
        public System.Windows.Forms.Label stateLabel;
        private System.Windows.Forms.MenuItem menuItem;
        private System.Windows.Forms.MenuItem exchangeMenuItem;
        private System.Windows.Forms.MenuItem showSettingsMenuItem;
        private System.Windows.Forms.MenuItem exitMenuItem;
    }
}