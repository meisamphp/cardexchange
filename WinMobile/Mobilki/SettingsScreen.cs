using System;
using System.Linq;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Text;
using System.Windows.Forms;
using Microsoft.WindowsMobile.PocketOutlook;

namespace Mobilki
{
    public partial class SettingsScreen : Form
    {
        private MainScreen s;

        public SettingsScreen(MainScreen s)
        {
            InitializeComponent();
            this.s = s;

            nameTextBox.Text = Settings.Name;
            phoneTextBox.Text = Settings.PhoneNumber;
        }

        private void saveData(object sender, EventArgs e)
        {
            Settings.Name = nameTextBox.Text;
            Settings.PhoneNumber = phoneTextBox.Text;
            Settings.Update();

            switchScreen();
        }

        private void switchScreen()
        {
            Hide();
            s.Show();
        }
    }
}
