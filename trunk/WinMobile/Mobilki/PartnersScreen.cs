using System;
using System.Linq;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Text;
using System.Windows.Forms;

namespace Mobilki
{
    public partial class PartnersScreen : Form
    {
        private MainScreen screen;
        private PairList partners;

        public PartnersScreen(PairList pairs, MainScreen scr)
        {
            InitializeComponent();
            this.screen = scr;
            this.partners = pairs;
            foreach (KeyValuePair<int, string> p in pairs)
            {
                MenuItem menuItem = new MenuItem();
                menuItem.Text = p.Value;
                chooseMenuItem.MenuItems.Add(menuItem);
                menuItem.Click += new System.EventHandler(this.pairClicked);
            }
        }

        public void pairClicked(object sender, EventArgs e)
        {
            string name = ((MenuItem)sender).Text;
            foreach (KeyValuePair<int, string> p in partners)
            {
                if (((string)p.Value).Equals(name))
                {
                    screen.SelectedPairId = (int)p.Key;
                    exit(null, null);
                }
            }
        }

        public void exit(object sender, EventArgs e)
        {
            this.Hide();
            screen.Show();
        }

    }
}