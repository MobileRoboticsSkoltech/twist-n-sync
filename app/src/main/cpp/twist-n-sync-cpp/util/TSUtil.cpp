#include "TSUtil.h"
#include "unsupported/Eigen/FFT"
#include "unsupported/Eigen/Polynomials"
#include "util/CubicSpline.h"

#include <numeric>

namespace tsutil {

    Eigen::VectorXd arangeEigen(double start, double const & stop, double const & step){
        Eigen::Index size = std::ceil((stop - start) / step);
        Eigen::VectorXd result(size);
        for (Eigen::Index i = 0; i < size; ++i) {
            result[i] = start;
            start += step;
        }
        return result;
    }

    Eigen::VectorXd adjDiffEigen(const Eigen::VectorXd &data) {
        Eigen::VectorXd new_data(data.size());
        std::adjacent_difference(data.begin(), data.end(), new_data.begin());

        return new_data(Eigen::seq(1, Eigen::last));
    }

    Eigen::VectorXd vectorToEigVectorXd(std::vector<double> data) {
        return Eigen::Map<Eigen::VectorXd>(data.data(), static_cast<Eigen::Index> (data.size()));
    }

    Eigen::MatrixX3d vectorToEigMatrixX3d(std::vector<std::vector<double>> data) {
        Eigen::MatrixX3d eigen_data(data.size(), 3);
        for (Eigen::Index i = 0; i < data.size(); ++i)
            eigen_data.row(i) = Eigen::Map<Eigen::Vector3d>(data[i].data(), 3);
        return eigen_data;
    }

    Eigen::VectorXd getNormOfRows(Eigen::MatrixX3d const & data){
        Eigen::VectorXd norm_data(data.rows());
        Eigen::Index i = 0;
        for (auto row : data.rowwise()){
            norm_data[i++] = row.norm();
        }
        return norm_data;
    }

    Eigen::VectorXd interpolate(Eigen::VectorXd const & x_old, Eigen::VectorXd const & y_old,
                                Eigen::VectorXd const & x_new){

        CubicSpline interp(x_old, y_old);
        return interp.getValuesOnSegment(x_new);
    }

    Eigen::Vector2d quadraticRoots(Eigen::VectorXd const & coeffs){
        Eigen::PolynomialSolver<double, 2> solver(coeffs);
        return solver.roots().real();
    }

    Eigen::VectorXd eigenCrossCor(Eigen::VectorXd & data_1, Eigen::VectorXd & data_2){
        // Cross-cor(x, y) = iFFT(FFT(x) * conj(FFT(y)))

        Eigen::Index old_data1_size = data_1.size();
        Eigen::Index old_data2_size = data_2.size();

        // Length of Discrete Fourier Transform
        Eigen::Index N = old_data1_size + old_data2_size - 1;

        N = static_cast<Eigen::Index> (std::pow(2, std::ceil(std::log2(N))));

        Eigen::Index shift = N - (old_data1_size + old_data2_size - 1);

        // Zero padding both vectors

        data_1.conservativeResize(N);
        data_2.conservativeResize(N);
        for (Eigen::Index i = old_data1_size; i < N; ++i) data_1[i] = 0.0;
        for (Eigen::Index i = old_data2_size; i < N; ++i) data_2[i] = 0.0;

        Eigen::FFT <double> fft;

        Eigen::VectorXcd fft_first(N);
        Eigen::VectorXcd fft_second(N);

        fft.fwd(fft_first, data_1);
        fft.fwd(fft_second, data_2);


        Eigen::VectorXcd fft_result = fft_first.array() * fft_second.conjugate().array();

        Eigen::VectorXd unbiased_result(N);
        fft.inv(unbiased_result, fft_result);

        // Rotating cross-correlation vector on shift
        Eigen::VectorXd cross_cor(N);
        cross_cor << unbiased_result(Eigen::seq(old_data1_size, Eigen::last)),
        unbiased_result(Eigen::seq(0, old_data1_size - 1));

        return cross_cor(Eigen::seq(shift, Eigen::last));
    }
}